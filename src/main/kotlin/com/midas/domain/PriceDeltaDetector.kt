package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.CurrentSystemOffsetRepository
import com.midas.repositories.PriceChangeMilestoneRepository
//import com.midas.repositories.TimeWindowEntryRepository
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.collections.HashMap

class PriceDeltaDetector {
    @Component
    private class SpringAdapter(
        @Autowired private val applicationProperties        : ApplicationProperties,
        @Autowired private val priceChangeMilestoneRepository: PriceChangeMilestoneRepository,
        @Autowired private val currentSystemOffsetRepository: CurrentSystemOffsetRepository,
        @Autowired private val loggingService               : LoggingService
    ) {
        @PostConstruct
        fun init() {
            PriceDeltaDetector.currentSystemOffsetRepository    = currentSystemOffsetRepository
            PriceDeltaDetector.applicationProperties            = applicationProperties
            PriceDeltaDetector.loggingService                   = loggingService
            PriceChangeMilestone.priceChangeMilestoneRepository = priceChangeMilestoneRepository

            CurrentSystemOffset.init()
            PriceChangeMilestone.init()
            TimeWindow.init()
            loggingService.log("DeltaRanker initialized")
        }
    }
    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var currentSystemOffsetRepository: CurrentSystemOffsetRepository
        private val latestRankings = mutableListOf<PriceChangeMilestone>()
        private val tickerGroups   = mutableMapOf<String, TickerGroup>()
        var windowSizesForTestMode = false
        private lateinit var loggingService: LoggingService

        fun rank(date: Date, stocks: List<Pair<String, Double>>): List<PriceChangeMilestone> {
            latestRankings.clear()

            stocks.forEach {
                rank(
                    ticker = it.first,
                    price  = it.second,
                    date   = date
                )
            }

            CurrentSystemOffset.increment()
            loggingService.log("Ranking completed for ${CurrentSystemOffset.get() - 1}. Starting ranking for offset ${CurrentSystemOffset.get()}")
            return latestRankings
        }

        private fun rank(ticker: String, price: Double, date: Date) {
            if(price <=0) {
                return
            }

            /** If no entry exists for ticker then load a queue for each ticker with each queue representing a window
             * **/
            val tickerGroup = tickerGroups.computeIfAbsent(
                ticker,
            ) {
                TickerGroup(ticker = ticker)
            }
            tickerGroup.rank(price = price, date = date)
        }

        /** If this ever makes it into production I'm sure it will be because of autocomplete **/
        fun clearAllDataOnlyForIntegrationTests() {
            if (applicationProperties.isNotIntegrationTest) {
                throw RuntimeException("Calling clear on delta ranker from outside an integration test")
            }
            latestRankings.clear()
            tickerGroups.clear()
            PriceChangeMilestone.clear()
            TimeWindow.clear()
            CurrentSystemOffset.clear()
        }
    }

    private class TickerGroup {
        private val timeWindows: MutableList<TimeWindow> = mutableListOf()
        constructor(ticker: String) {
            initWindowGroups(ticker = ticker)
        }

        fun initWindowGroups(ticker: String) {
            /** Without this the integration tests as they are would be too large and complicated **/
            if(windowSizesForTestMode) {
                timeWindows.add(TimeWindow(ticker = ticker, size = 2))
                for (i in 1 until 13) {
                    timeWindows.add(
                        TimeWindow(ticker = ticker, size = 5 * i)
                    )
                }

            } else {
                timeWindows.add(
                    TimeWindow(ticker = ticker, size = 5)
                )
                timeWindows.add(
                    TimeWindow(ticker = ticker, size = 10)
                )
                timeWindows.add(
                    TimeWindow(ticker = ticker, size = 15)
                )
                timeWindows.add(
                    TimeWindow(ticker = ticker, size = 30)
                )
                timeWindows.add(
                    TimeWindow(ticker = ticker, size = 60)
                )
                timeWindows.add(
                    TimeWindow(ticker = ticker, size = 120)
                )
                timeWindows.add(
                    TimeWindow(ticker = ticker, size = 180)
                )
            }
        }

        fun rank(price: Double, date: Date) {
            timeWindows.forEach{ it.rank(price = price, date = date)}
        }
    }
    private class TimeWindow {
        val ticker: String
        val size: Int

        val queue: Queue<TimeWindowEntry> = LinkedBlockingQueue()
        constructor(ticker: String, size: Int) {
            this.size   = size
            this.ticker = ticker

            val entries: List<TimeWindowEntry> =
                timeWindowEntries["$ticker-$size"]?.sortedBy { it.currentOffset } ?: return

            for(i in entries.indices) {
                queue.add(entries[i])
            }
        }
        companion object {
            var timeWindowEntries: MutableMap<String, MutableList<TimeWindowEntry>> = HashMap()

            fun init() {
                loggingService.log("TimeWindow initialized")
            }

            fun clear() {
                timeWindowEntries.clear()
            }
        }

        fun rank(price: Double, date: Date) {
            val newEntry = TimeWindowEntry(
                    ticker        = ticker,
                    size          = size,
                    price         = price,
                    currentOffset = CurrentSystemOffset.get(),
                    creationTime  = date
                )

            queue.add(newEntry)
            if(queue.size < size) {
                return
            }

            val oldPrice = queue.remove()
            val newDelta = ((price - oldPrice.price)/oldPrice.price) * 100.0
            if(newDelta < 0) {
                return
            }

            PriceChangeMilestone.updateIfNewMilestone(
                ticker       = ticker,
                timeWindow   = size,
                newDelta     = newDelta,
                price        = price,
                creationTime = date
            )
        }
    }

    @Entity(name="CurrentSystemOffset")
    @Table(name="current_system_offset")
    class CurrentSystemOffset {
        @Id
        val id = -1L
        @Column
        var value: Long

        private constructor(value: Long) {
            this.value = value
        }

        companion object {
            private lateinit var currentSystemOffset: CurrentSystemOffset
            fun init()  {
                val results = currentSystemOffsetRepository.findAll().toList()
                if(results.isNotEmpty()) {
                    currentSystemOffset = results[0]
                    return
                }
                currentSystemOffset = currentSystemOffsetRepository.save(
                        CurrentSystemOffset(value = 0)
                )
            }

            fun increment() {
                currentSystemOffset.value++
                currentSystemOffset = currentSystemOffsetRepository.save(currentSystemOffset)
            }

            fun get() : Long {
                return currentSystemOffset.value
            }

            /** TODO: This is only for testing and should be removed ASAP **/
            fun clear() {
                currentSystemOffset.value = 0
                currentSystemOffset = currentSystemOffsetRepository.save(currentSystemOffset)
            }

        }
    }

    class TimeWindowEntry {
        val ticker: String
        val size: Int
        val price: Double
        val currentOffset: Long
        val creationTime: Date
        constructor(ticker: String, size: Int, price: Double, currentOffset: Long, creationTime: Date) {
            this.ticker        = ticker
            this.size          = size
            this.price         = price
            this.currentOffset = currentOffset
            this.creationTime  = creationTime
        }
    }

    @Entity(name="PriceChangeMilestone")
    @Table(name="price_change_milestone")
    class PriceChangeMilestone {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column
        private val id = -1
        @Column
        val ticker: String
        @Column
        val price: Double
        @Column
        val priceDelta: Double
        @Column
        val milestoneChange: Double
        @Column
        val timeWindow: Int
        @Column
        val distance: Long
        @Column
        val offset: Long
        @Column
        val currentOffset: Long
        @Column
        val probabilityCoefficient: Long
        @Column
        val creationTime: Date
        constructor(
                    ticker: String,
                    price: Double,
                    priceDelta: Double,
                    milestoneChange: Double,
                    timeWindow: Int,
                    distance: Long,
                    offset: Long,
                    currentOffset: Long,
                    creationTime: Date) {

            this.ticker                 = ticker
            this.price                  = price
            this.priceDelta             = priceDelta
            this.milestoneChange        = milestoneChange
            this.timeWindow             = timeWindow
            this.distance               = distance
            this.offset                 = offset
            this.currentOffset          = currentOffset
            this.probabilityCoefficient = this.timeWindow * this.distance
            this.creationTime           = creationTime
        }



        companion object {
            private  val milestonesByTickerAndWindow    : MutableMap<String, PriceChangeMilestone> = HashMap()
            lateinit var priceChangeMilestoneRepository : PriceChangeMilestoneRepository

            fun init() {
                val results = priceChangeMilestoneRepository.findAll()
                for(r in results) {
                    milestonesByTickerAndWindow.computeIfAbsent("${r.ticker}-${r.timeWindow}") {r}
                }
                loggingService.log("PriceChangeMilestone initialized")
            }
            fun clear() {
                this.milestonesByTickerAndWindow.clear()
                priceChangeMilestoneRepository.deleteAll()
            }

            fun updateIfNewMilestone(ticker: String, timeWindow: Int, newDelta: Double, price: Double, creationTime: Date) {
                val lastMilestone: PriceChangeMilestone? = milestonesByTickerAndWindow["$ticker-$timeWindow"]
                if (lastMilestone == null) {
                    val newMilestone = priceChangeMilestoneRepository.save(PriceChangeMilestone(
                        ticker            = ticker,
                        priceDelta        = newDelta,
                        price             = price,
                        milestoneChange   = 0.0,
                        timeWindow        = timeWindow,
                        distance          = CurrentSystemOffset.get(),
                        offset            = CurrentSystemOffset.get(),
                        currentOffset     = CurrentSystemOffset.get(),
                        creationTime      = creationTime
                    ))
                    milestonesByTickerAndWindow["$ticker-$timeWindow"] = newMilestone
                    priceChangeMilestoneRepository.save(newMilestone)
                    latestRankings.add(newMilestone)
                    return
                }

                if (lastMilestone.priceDelta >= newDelta) {
                    return
                }

                val updatedMilestone = PriceChangeMilestone(
                    ticker            = ticker,
                    priceDelta        = newDelta,
                    price             = price,
                    milestoneChange   = calculateMilestoneChange(newMilestone = newDelta, oldMilestone = lastMilestone.priceDelta),
                    timeWindow        = timeWindow,
                    distance          = CurrentSystemOffset.get() - lastMilestone.offset,
                    offset            = lastMilestone.offset,
                    currentOffset     = CurrentSystemOffset.get(),
                    creationTime      = creationTime
                )
                milestonesByTickerAndWindow["$ticker-$timeWindow"] = priceChangeMilestoneRepository.save(updatedMilestone)
                latestRankings.add(milestonesByTickerAndWindow["$ticker-$timeWindow"]!!)
                priceChangeMilestoneRepository.delete(lastMilestone)
            }
            private fun calculateMilestoneChange(newMilestone: Double, oldMilestone: Double) : Double {
                if(oldMilestone == 0.0) {
                    return 0.0 // was 100 but that is going to give false positives
                }
                return ((newMilestone - oldMilestone) / oldMilestone) * 100
            }
        }

        override fun toString() : String {
            return "ticker: $ticker, priceDelta: $priceDelta,distance: $distance,timeWindow: $timeWindow,  offset: $offset,  price: $price, milestoneChange: $milestoneChange"
        }
    }
}