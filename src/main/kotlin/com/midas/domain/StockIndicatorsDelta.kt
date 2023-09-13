package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.StockIndicatorsDeltaRepository
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

@Component
@Entity
@Table(name = "stock_indicators_delta")
class StockIndicatorsDelta {

    @Component
    private class SpringAdapter(
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val stockIndicatorsDeltaRepository: StockIndicatorsDeltaRepository
    ) {
        @PostConstruct
        fun init() {
            StockIndicatorsDelta.applicationProperties          = applicationProperties
            StockIndicatorsDelta.stockIndicatorsDeltaRepository = stockIndicatorsDeltaRepository
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var stockIndicatorsDeltaRepository: StockIndicatorsDeltaRepository
        fun save(deltas: StockIndicatorsDelta) : StockIndicatorsDelta{
            return stockIndicatorsDeltaRepository.save(deltas)
        }
    }



        /**** Database entity information begins --------------------------------------------------------------------**/
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private val id: Long = -1

    @Column
    private val ticker: String

    @Column
    private val priceChangePerCent: Double
    @Column
    private val volumeChangePerCent: Double
    @Column
    private val vwapChangePercent: Double

    @Column
    private val hour: Int
    @Column
    private val minute: Int
    @Column
    private val externalTime: Long
    @Column
    private val creationDate: Date

    constructor(
        ticker: String,
        priceChangePercent: Double,
        volumeChangePercent: Double,
        vwapChangePercent: Double,
        hour: Int,
        minute: Int,
        externalTime: Long,
        creationDate: Date,
    ) {
        this.ticker              = ticker
        this.priceChangePerCent  = priceChangePercent
        this.volumeChangePerCent = volumeChangePercent
        this.vwapChangePercent   = vwapChangePercent
        this.hour                = hour
        this.minute              = minute
        this.externalTime        = externalTime
        this.creationDate        = creationDate
    }

    /**** Database entity information ends --------------------------------------------------------------------**/
}
