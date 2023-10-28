package com.midas.domain

import com.midas.repositories.MilestoneRepository
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
    private val id           : Long = -1L
    private val ticker       : String
    private val maxDelta     : Double
    private val minDelta     : Double
    private val maxPrice     : Double
    private val minPrice     : Double
    private val averageVolume: Double
    private val windowDelta  : Double
    private val timeWindow   : Int
    private val count        : Int /** TOOD: Polygon API has some tickers that have very few records but it might not be polygons fault**/
    constructor(
            ticker       : String,
            minPrice     : Double,
            maxPrice     : Double,
            maxDelta     : Double,
            minDelta     : Double,
            averageVolume: Double,
            windowDelta  : Double,
            timeWindow   : Int,
            count        : Int
    ) {
        this.ticker        = ticker
        this.minPrice      = minPrice
        this.maxPrice      = maxPrice
        this.maxDelta      = maxDelta
        this.minDelta      = minDelta
        this.averageVolume = averageVolume
        this.windowDelta   = windowDelta
        this.timeWindow    = timeWindow
        this.count         = count
    }

    @Component
    class SpringAdapter(
        @Autowired private val milestoneRepository: MilestoneRepository
    ) {
        @PostConstruct
        fun init() {
            Milestone.milestoneRepository = milestoneRepository
        }

    }
    companion object {
        private lateinit var milestoneRepository: MilestoneRepository

        fun save(m: Milestone) {
            milestoneRepository.save(m)
        }
    }
}