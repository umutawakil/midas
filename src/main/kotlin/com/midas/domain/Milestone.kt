package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.MilestoneRepository
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Entity
@Table(name="milestone")
class Milestone {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id  : Long = -1L
    private val ticker: String
    private val maxDelta: Double
    private val maxPrice: Double
    private val timeWindow: Int
    constructor(ticker: String, maxPrice: Double, maxDelta: Double, timeWindow: Int) {
        this.ticker     = ticker
        this.maxPrice   = maxPrice
        this.maxDelta   = maxDelta
        this.timeWindow = timeWindow
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
        private val WINDOWS: List<Int> = listOf(5,10,20)

        fun calculateMilestones() {
            var c = 0
            for (t: String in Ticker.getTickers()) {
                loggingService.log("Ticker: $c")
                c++
                val snapshots: List<StockSnapshot> = StockSnapshot.findByDescending(ticker = t)
                for (w in WINDOWS) {
                    var maxDelta = 0.0
                    var maxPrice = 0.0

                    for (i in 1 until w) {
                        if (i >= snapshots.size) {break}
                        val currentDelta = StockSnapshot.delta(x2 = snapshots[i], x1 = snapshots[i - 1])
                        if (currentDelta > maxDelta) {
                            maxDelta =  currentDelta
                        }
                        maxPrice = StockSnapshot.max(currentPrice = maxPrice, s = snapshots[i])
                    }
                    milestoneRepository.save(
                        Milestone(ticker = t, maxPrice = maxPrice, maxDelta = maxDelta, timeWindow = w)
                    )

                }
            }
        }
    }
}