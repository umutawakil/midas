package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.PriceChangeMilestoneRepository
import com.midas.repositories.TimeWindowEntryRepository
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
    class SpringAdapter(
        @Autowired private val applicationProperties        : ApplicationProperties,
        @Autowired private val priceChangeMilestoneRepository: PriceChangeMilestoneRepository,
        @Autowired private val timeWindowEntryRepository: TimeWindowEntryRepository,
        @Autowired private val loggingService               : LoggingService
    ) {
        @PostConstruct
        fun init() {
            TimeWindow.timeWindowEntryRepository                = timeWindowEntryRepository
            PriceChangeMilestone.priceChangeMilestoneRepository = priceChangeMilestoneRepository
            PriceDeltaDetector.loggingService                          = loggingService
            PriceChangeMilestone.init()
            TimeWindow.init()

            loggingService.log("DeltaRanker initialized")
        }
    }
    companion object {
        private val latestRankings = mutableListOf<PriceChangeMilestone>()
        private val tickerGroups   = mutableMapOf<String, TickerGroup>()
        var windowSizesForTestMode = false
        lateinit var loggingService: LoggingService

        var currentSystemOffset = 0L //Inits initialized inside the TimeWindow class
        private var currentDate: Date? = null

        fun rank(stocks: List<Pair<String, Double>>): List<PriceChangeMilestone> {
            latestRankings.clear()
            currentDate = Date(0)
            stocks.forEach {
                rank(
                    ticker = it.first,
                    price  = it.second
                )
            }

            currentSystemOffset++
            return latestRankings
        }

        private fun rank(ticker: String, price: Double) {
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
            tickerGroup.rank(price = price)
        }

        /** If this ever makes it into production I'm sure it will be because of autocomplete **/
        fun clearAllDataOnlyForIntegrationTests() {
            latestRankings.clear()
            tickerGroups.clear()
            PriceChangeMilestone.clear()
            TimeWindow.clear()
            currentSystemOffset = 0
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
                loggingService.log("Initializing window groups with test mode settings")
                timeWindows.add(TimeWindow(ticker = ticker, size = 2))
                for (i in 1 until 13) {
                    timeWindows.add(
                        TimeWindow(ticker = ticker, size = 5 * i)
                    )
                }

            } else {
                loggingService.log("Initializing window groups normal mode settings")
                timeWindows.add(
                    TimeWindow(ticker = ticker, size = 15)
                )

                timeWindows.add(
                    TimeWindow(ticker = ticker, size = 30)
                )
                timeWindows.add(
                    TimeWindow(ticker = ticker, size = 60)
                )
            }
        }

        fun rank(price: Double) {
            timeWindows.forEach{ it.rank(price = price)}
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
            lateinit var timeWindowEntryRepository     : TimeWindowEntryRepository
            var timeWindowEntries: MutableMap<String, MutableList<TimeWindowEntry>> = HashMap()

            fun init() {
                val results: List<TimeWindowEntry> = timeWindowEntryRepository.findAll().toList()
                initSystemOffset(results)

                for(r in results) {
                    timeWindowEntries.computeIfAbsent("${r.ticker}-${r.size}"){mutableListOf()}.add(r)
                }
            }

            fun initSystemOffset(results: List<TimeWindowEntry>) {
                if(results.isEmpty()) {
                    currentSystemOffset = 0L
                } else {
                    currentSystemOffset = results[0].currentOffset
                    for(i in results.indices) {
                        if(currentSystemOffset < results[i].currentOffset) {
                            currentSystemOffset = results[i].currentOffset
                        }
                    }
                    currentSystemOffset += 1
                }
                loggingService.log("CurrentOffset: ${currentSystemOffset}")
            }

            fun clear() {
                timeWindowEntries.clear()
                timeWindowEntryRepository.deleteAll()
            }
        }

        fun rank(price: Double) {
            val newEntry = timeWindowEntryRepository.save(
                TimeWindowEntry(
                    ticker        = ticker,
                    size          = size,
                    price         = price,
                    currentOffset = currentSystemOffset
                )
            )
            queue.add(newEntry)
            if(queue.size < size) {
                return
            }

            val oldPrice = queue.remove()
            timeWindowEntryRepository.delete(oldPrice)
            //queue.add(price)
            val newDelta = ((price - oldPrice.price)/oldPrice.price) * 100.0
            if(newDelta < 0) {
                return
            }

            PriceChangeMilestone.updateIfNewMilestone(
                ticker     = ticker,
                timeWindow = size,
                newDelta   = newDelta,
                price      = price
            )
        }
    }

    @Entity(name="TimeWindowEntry")
    @Table(name="time_window_entry")
    class TimeWindowEntry {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column
        val id = -1
        @Column
        val ticker: String
        @Column
        val size: Int
        @Column
        val price: Double
        @Column
        val currentOffset: Long
        constructor(ticker: String, size: Int, price: Double, currentOffset: Long) {
            this.ticker        = ticker
            this.size          = size
            this.price         = price
            this.currentOffset = currentOffset
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
            }
            fun clear() {
                this.milestonesByTickerAndWindow.clear()
                priceChangeMilestoneRepository.deleteAll()
            }

            fun updateIfNewMilestone(ticker: String, timeWindow: Int, newDelta: Double, price: Double) {
                val lastMilestone: PriceChangeMilestone? = milestonesByTickerAndWindow["$ticker-$timeWindow"]
                if (lastMilestone == null) {
                    val newMilestone = priceChangeMilestoneRepository.save(PriceChangeMilestone(
                        ticker            = ticker,
                        priceDelta        = newDelta,
                        price             = price,
                        milestoneChange   = 0.0,
                        timeWindow        = timeWindow,
                        distance          = currentSystemOffset,
                        offset            = currentSystemOffset,
                        currentOffset     = currentSystemOffset,
                        creationTime      = currentDate!!
                    ))
                    milestonesByTickerAndWindow["$ticker-$timeWindow"] = newMilestone
                    priceChangeMilestoneRepository.save(newMilestone)
                    latestRankings.add(newMilestone)
                    return
                }

                if(lastMilestone.priceDelta >= newDelta) {
                    return
                }

                val updatedMilestone = PriceChangeMilestone(
                    ticker            = ticker,
                    priceDelta        = newDelta,
                    price             = price,
                    milestoneChange   = calculateMilestoneChange(newMilestone = newDelta, oldMilestone = lastMilestone.priceDelta),
                    timeWindow        = timeWindow,
                    distance          = currentSystemOffset - lastMilestone.offset,
                    offset            = lastMilestone.offset,
                    currentOffset     = currentSystemOffset,
                    creationTime      = currentDate!!
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