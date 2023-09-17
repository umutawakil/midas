package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.DeltasOfStockIndicatorsRepository
import com.midas.utilities.DomainValueCompareUtil
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

@Component
@Entity
@Table(name = "deltas_of_stock_indicators")
class DeltasOfStockIndicators {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private val id: Long = -1

    @Column
    private val ticker: String

    @Column
    private val priceChangePercent: Double
    @Column
    private val volumeChangePercent: Double
    @Column
    private val volumePriceDeltaRatio: Double
    @Column
    private val volumePriceDeltaLag: Double
    @Column
    private val vwapChangePercent: Double
    @Column
    private val volatilityEstimate: Double

    @Column
    private val priceDeltaVolatilityRatio: Double
    @Column
    private val priceDeltaVolatilityDiff: Double
    @Column
    private val runningVolatilityEstimate: Double
    @Column
    private val todayPreviousVolatilityDelta: Double

    @Column
    private val hour: Int
    @Column
    private val minute: Int
    @Column
    private val externalTime: Long
    @Temporal(TemporalType.DATE)
    @Column
    private val creationDate: Date

    constructor(
            ticker: String,
            priceChangePercent: Double,
            volumeChangePercent: Double,
            vwapChangePercent: Double,
            volumePriceDeltaRatio: Double,
            volatilityEstimate: Double,
            priceDeltaVolatilityRatio: Double,
            priceDeltaVolatilityDiff: Double,
            runningVolatilityEstimate: Double,
            todayPreviousVolatilityDelta: Double,
            hour: Int,
            minute: Int,
            externalTime: Long,
            creationDate: Date,
    ) {
        this.ticker                          = ticker
        this.priceChangePercent              = priceChangePercent
        this.volumeChangePercent             = volumeChangePercent
        this.vwapChangePercent               = vwapChangePercent
        this.volumePriceDeltaRatio           = volumePriceDeltaRatio//if(priceChangePercent == 0.0) { Double.POSITIVE_INFINITY} else {((volumeChangePercent /priceChangePercent)*100)}
        this.volumePriceDeltaLag             = volumeChangePercent - priceChangePercent
        this.volatilityEstimate              = volatilityEstimate
        this.priceDeltaVolatilityRatio       = priceDeltaVolatilityRatio
        this.priceDeltaVolatilityDiff        = priceDeltaVolatilityDiff
        this.runningVolatilityEstimate       = runningVolatilityEstimate
        this.todayPreviousVolatilityDelta    = todayPreviousVolatilityDelta
        this.hour                            = hour
        this.minute                          = minute
        this.externalTime                    = externalTime
        this.creationDate                    = creationDate
    }

    @Component
    private class SpringAdapter(
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val deltasOfStockIndicatorsRepository: DeltasOfStockIndicatorsRepository
    ) {
        @PostConstruct
        fun init() {
            DeltasOfStockIndicators.applicationProperties             = applicationProperties
            DeltasOfStockIndicators.deltasOfStockIndicatorsRepository = deltasOfStockIndicatorsRepository
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var deltasOfStockIndicatorsRepository: DeltasOfStockIndicatorsRepository

        fun save(deltas: DeltasOfStockIndicators) : DeltasOfStockIndicators{
            return deltasOfStockIndicatorsRepository.save(deltas)
        }

        fun deleteAll() {
            if(!applicationProperties.isIntegrationTest) {
                throw RuntimeException("data not deleted. This is not running in an integration environment")
            }
            deltasOfStockIndicatorsRepository.deleteAll()
        }

        /*** Test code --------------------------------------------------**/
        fun validateEntry(
            date: Date,
            position: Int,
            referenceEntry: DeltasOfStockIndicators
        )  {
            val deltasToValidate = deltasOfStockIndicatorsRepository.findAllByCreationDate(date)[position]
            DomainValueCompareUtil.equalInValue(
                object1 = referenceEntry,
                object2 = deltasToValidate
            )
        }

        fun entriesExistForDate(date: Date) : Boolean {
            return deltasOfStockIndicatorsRepository.findAllByCreationDate(creationDate = date).isNotEmpty()
        }
    }
}
