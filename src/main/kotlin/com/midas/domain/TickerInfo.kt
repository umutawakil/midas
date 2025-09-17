package com.midas.domain

import com.midas.repositories.TickerInfoRepository
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


/**
 * This class provides a way to join tickers with their EDGAR data
 */

@Entity
@Table(name="ticker_info")
class TickerInfo {
    @Id
    @Column
    private val ticker         : String
    private val secSectorCode  : Int
    private val sicCode        : String
    private val cik            : Long
    private val name           : String
    private val otc            : Boolean
    private val financialData  : Boolean

    constructor(
        ticker: String,
        secSectorCode: Int,
        sicCode: String,
        cik: Long,
        name: String,
        otc: Boolean,
        financialData: Boolean
    ) {
        this.ticker        = ticker
        this.secSectorCode = secSectorCode
        this.sicCode       = sicCode
        this.cik           = cik
        this.name          = name
        this.otc           = otc
        this.financialData = financialData
    }

    @Component
    class SpringAdapter(
        @Autowired private val tickerInfoRepository: TickerInfoRepository
    ) {
        @PostConstruct
        fun init() {
            TickerInfo.tickerInfoRepository = tickerInfoRepository
        }
    }

    companion object {
        private lateinit var tickerInfoRepository: TickerInfoRepository
        fun save(t: TickerInfo) {
            tickerInfoRepository.save(t)
        }
    }
}