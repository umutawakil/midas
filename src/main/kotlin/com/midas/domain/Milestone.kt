package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.MilestoneRepository
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import kotlin.math.min

@Entity
@Table(name="milestone")
class Milestone {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id  : Long = -1L
    private val ticker: String
    private val maxDelta: Double
    private val minDelta: Double
    private val maxPrice: Double
    private val minPrice: Double
    private val windowDelta: Double
    private val timeWindow: Int
    private val count: Int /** TOOD: Polygon API has some tickers that have very few records but it might not be polygons fault**/
    constructor(ticker: String, minPrice: Double, maxPrice: Double, maxDelta: Double, minDelta: Double, windowDelta: Double, timeWindow: Int, count: Int) {
        this.ticker      = ticker
        this.minPrice    = minPrice
        this.maxPrice    = maxPrice
        this.maxDelta    = maxDelta
        this.minDelta    = minDelta
        this.windowDelta = windowDelta
        this.timeWindow  = timeWindow
        this.count       = count
    }

    @Component
    class SpringAdapter(
        @Autowired private val milestoneRepository: MilestoneRepository,
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val loggingService: LoggingService,
        @Autowired private val tickerSpringAdapter: Ticker.SpringAdapter
    ) {
        @PostConstruct
        fun init() {
            Milestone.applicationProperties = applicationProperties
            Milestone.milestoneRepository   = milestoneRepository
            Milestone.loggingService        = loggingService
            tickerSpringAdapter.init()

        }

    }
    companion object {
        private lateinit var milestoneRepository: MilestoneRepository
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var loggingService: LoggingService
        private val WINDOWS: List<Int> = listOf(5, 10, 20, 40, 60)

        fun calculateMilestones() {
            var c = 0
            for (t: String in Ticker.getTickers()) { loggingService.log("Ticker: $c")
                c++
                val snapshots: List<StockSnapshot> = StockSnapshot.findByDescending(ticker = t)
                for (w in WINDOWS) {
                    var maxDelta = 0.0
                    var maxPrice = 0.0
                    var minPrice = Double.MAX_VALUE
                    var minDelta = Double.MAX_VALUE

                    for (i in 1 until w) {
                        if (i >= snapshots.size) {break}
                        val currentDelta = StockSnapshot.delta(x2 = snapshots[i - 1], x1 = snapshots[i])
                        if (currentDelta > maxDelta) {
                            maxDelta = currentDelta
                        }
                        if (currentDelta < minDelta) {
                            minDelta = currentDelta
                        }

                        maxPrice = StockSnapshot.max(currentPrice = maxPrice, s = snapshots[i - 1])
                        minPrice = StockSnapshot.min(currentPrice = minPrice, s = snapshots[i - 1])
                    }

                    var windowDelta = 0.0
                    if(snapshots.size >= w) {
                        windowDelta = StockSnapshot.delta(x2 = snapshots[0], x1 = snapshots[w - 1])
                    }
                    milestoneRepository.save(
                        Milestone(
                            ticker      = t,
                            minPrice    = minPrice,
                            maxPrice    = maxPrice,
                            maxDelta    = maxDelta,
                            minDelta    = minDelta,
                            windowDelta = windowDelta,
                            timeWindow  = w,
                            count       = snapshots.size
                        )
                    )
                }
            }
        }
    }
}