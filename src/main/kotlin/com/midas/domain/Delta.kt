package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.DeltaRepository
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Entity
@Table(name="delta")
class Delta {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id  : Long = -1L
    private val ticker: String
    private val price: Double
    private val delta: Double
    private val runningDelta: Double
    private val previousClosePrice: Double
    private val openPrice: Double
    constructor(
        ticker: String,
        price: Double,
        delta: Double,
        runningDelta: Double,
        previousClosePrice: Double,
        openPrice: Double
    ) {
        this.ticker             = ticker
        this.price              = price
        this.delta              = delta
        this.runningDelta       = runningDelta
        this.previousClosePrice = previousClosePrice
        this.openPrice          = openPrice
    }

    @Component
    class SpringAdapter(
        @Autowired val applicationProperties : ApplicationProperties,
        @Autowired val deltaRepository       : DeltaRepository,
        @Autowired val loggingService        : LoggingService
    ) {
        @PostConstruct
        fun init() {
            Delta.applicationProperties = applicationProperties
            Delta.deltaRepository       = deltaRepository
            Delta.loggingService        = loggingService
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var loggingService: LoggingService
        private lateinit var deltaRepository: DeltaRepository
        fun save(delta: Delta) {
            deltaRepository.save(delta)
        }
    }
}