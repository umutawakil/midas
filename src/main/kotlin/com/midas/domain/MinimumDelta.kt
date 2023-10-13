package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.MinimumDeltaRepository
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.stereotype.Component

@Entity
@Table(name="minimum_delta")
class MinimumDelta {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id          : Long = -1L
    private val ticker      : String
    private val minimumDelta: Double
    private val windowSize  : Int
    private val timeSpan    : Int

    constructor(
        ticker      : String,
        minimumDelta: Double,
        windowSize  : Int,
        timeSpan    : Int
    ) {
        this.ticker       = ticker
        this.minimumDelta = minimumDelta
        this.windowSize   = windowSize
        this.timeSpan     = timeSpan
    }

    @Component
    class SpringAdapter(
        private val applicationProperties     : ApplicationProperties,
        private val minimumDeltaRepository    : MinimumDeltaRepository,
        private val loggingService            : LoggingService,
        private val tickerSpringAdapter       : Ticker.SpringAdapter,
        private val stockSnapshotSpringAdapter: StockSnapshot.SpringAdapter
    ) {
        @PostConstruct
        fun init() {
            MinimumDelta.minimumDeltaRepository = minimumDeltaRepository
            MinimumDelta.applicationProperties  = applicationProperties
            MinimumDelta.loggingService         = loggingService

            tickerSpringAdapter.init()
            stockSnapshotSpringAdapter.init()
        }
    }

    companion object {
        private lateinit var minimumDeltaRepository : MinimumDeltaRepository
        private lateinit var applicationProperties  : ApplicationProperties
        private lateinit var loggingService         : LoggingService

        private val WINDOWS = listOf(5, 20)
        private val RANGES  = listOf(20, 60, 120, 240)
        fun calculate() {
            var counter = 0
            for(t: String in Ticker.getTickers()) {
                counter++
                loggingService.log("Counter ($t): $counter")
                val snapshots: List<StockSnapshot> = StockSnapshot.findByDescending(ticker = t)

                for (r in RANGES) {
                    if (snapshots.size <  r) { continue }

                    for (w in WINDOWS) {
                        if (snapshots.size < w) { continue }

                        var min: Double = Double.MAX_VALUE
                        var i = w
                        while (i < r) {
                            val delta = StockSnapshot.delta(x2 = snapshots[i - w], x1 = snapshots[i])
                            if (delta < min) {
                                min = delta
                            }
                            i += w
                        }
                        minimumDeltaRepository.save (
                            MinimumDelta(
                                ticker       = t,
                                minimumDelta = min,
                                windowSize   = w,
                                timeSpan     = r
                            )
                        )
                    }
                }
            }
        }
    }
}