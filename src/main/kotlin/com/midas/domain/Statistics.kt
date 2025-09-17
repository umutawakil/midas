package com.midas.domain

import com.midas.repositories.StatisticsRepository
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * This is the central domain object that all the jobs help build.
 */

@Entity
@Table(name="statistics")
class Statistics {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id                  : Long = -1L
    private val ticker              : String
    private val maxDelta            : Double
    private val minDelta            : Double
    private val currentPrice        : Double
    private val currentPriceDelta   : Double
    private val currentVolumeDelta  : Double
    private val previousPriceDelta  : Double
    private val previousVolumeDelta : Double
    private val weeksPositive       : Int
    private val maxPrice            : Double
    private val minPrice            : Double
    private val averageVolume       : Double
    private val volumeDelta         : Double
    private val averageDelta        : Double
    private val averageDeviation    : Double
    private val windowDelta         : Double
    private val timeWindow          : Int
    private val count               : Int /** TODO: Polygon API has some tickers that have very few records but it might not be polygons fault**/
    constructor(
            ticker              : String,
            minPrice            : Double,
            maxPrice            : Double,
            maxDelta            : Double,
            currentPrice        : Double,
            minDelta            : Double,
            averageVolume       : Double,
            volumeDelta         : Double,
            averageDelta        : Double,
            currentPriceDelta   : Double,
            currentVolumeDelta  : Double,
            previousPriceDelta  : Double,
            previousVolumeDelta : Double,
            weeksPositive       : Int,
            averageDeviation    : Double,
            windowDelta         : Double,
            timeWindow          : Int,
            count               : Int
    ) {
        this.ticker              = ticker
        this.minPrice            = minPrice
        this.maxPrice            = maxPrice
        this.currentPrice        = currentPrice
        this.currentPriceDelta   = currentPriceDelta
        this.currentVolumeDelta  = currentVolumeDelta
        this.previousVolumeDelta = previousVolumeDelta
        this.previousPriceDelta  = previousPriceDelta
        this.weeksPositive       = weeksPositive
        this.maxDelta            = maxDelta
        this.minDelta            = minDelta
        this.averageDelta        = averageDelta
        this.averageDeviation    = averageDeviation
        this.averageVolume       = averageVolume
        this.volumeDelta         = volumeDelta
        this.windowDelta         = windowDelta
        this.timeWindow          = timeWindow
        this.count               = count
    }

    @Component
    class SpringAdapter(
        @Autowired private val statisticsRepository: StatisticsRepository
    ) {
        @PostConstruct
        fun init() {
            Statistics.statisticsRepository = statisticsRepository
        }
    }
    companion object {
        private lateinit var statisticsRepository: StatisticsRepository

        fun save(m: Statistics) {
            statisticsRepository.save(m)
        }
    }
}