package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.DeltaStatRepository
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.stereotype.Component
import kotlin.math.abs

@Entity
@Table(name="delta_stat")
class DeltaStat {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long = -1L
    private val ticker: String
    private val averageDelta: Double
    private val standardDeviationOfChange: Double
    private val windowDelta: Double
    private val windowSize: Int

    constructor(
        ticker: String,
        averageDelta: Double,
        standardDeviationOfChange: Double,
        windowDelta: Double,
        windowSize: Int
    ) {
        this.ticker                    = ticker
        this.averageDelta              = averageDelta
        this.standardDeviationOfChange = standardDeviationOfChange
        this.windowDelta               = windowDelta
        this.windowSize                = windowSize
    }

    @Component
    class SpringAdapter(
        private val applicationProperties: ApplicationProperties,
        private val deltaStatRepository: DeltaStatRepository,
        private val loggingService: LoggingService,
        private val tickerSpringAdapter: Ticker.SpringAdapter,
        private val stockSnapshotSpringAdapter: StockSnapshot.SpringAdapter
    ) {
        @PostConstruct
        fun init() {
            DeltaStat.deltaStatRepository   = deltaStatRepository
            DeltaStat.applicationProperties = applicationProperties
            DeltaStat.loggingService        = loggingService

            tickerSpringAdapter.init()
            stockSnapshotSpringAdapter.init()
        }
    }
    companion object {
        private lateinit var deltaStatRepository: DeltaStatRepository
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var loggingService: LoggingService
        private val WINDOWS = listOf(5, 10, 20, 60, 120, 240)
        fun calculateDeltaStats() {

            var counter = 0
            for(t: String in Ticker.getTickers()) {
                counter++
                loggingService.log("Counter ($t): $counter")
                val snapshots: List<StockSnapshot> = StockSnapshot.findByDescending(ticker = t)

                for(w in WINDOWS) {
                    if (snapshots.size < w) {
                        continue
                    }
                    var averageDelta = 0.0
                    for (i in 1 until w) {
                        averageDelta += StockSnapshot.delta(x2 = snapshots[i], x1 = snapshots[i - 1])
                    }
                    averageDelta /= w

                    var standardDeviationOfChange = 0.0
                    for (i in 1 until w) {
                        standardDeviationOfChange += abs(averageDelta - StockSnapshot.delta(x2 = snapshots[i], x1 = snapshots[i - 1]))
                    }
                    standardDeviationOfChange /= w

                    val windowDelta: Double = StockSnapshot.delta(x2 = snapshots[0], x1 = snapshots[w - 1])

                    deltaStatRepository.save(
                        DeltaStat(
                            ticker                    = t,
                            averageDelta              = averageDelta,
                            standardDeviationOfChange = standardDeviationOfChange,
                            windowDelta               = windowDelta,
                            windowSize                = w
                        )
                    )
                }
            }
        }
    }
}