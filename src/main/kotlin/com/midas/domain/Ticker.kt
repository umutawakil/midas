package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.TickerRepository
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
@Entity
@Table(name = "ticker")
class Ticker {
    @Component
    private class SpringAdapter(
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val tickerRepository: TickerRepository
    ) {
        @PostConstruct
        fun init() {
            Ticker.applicationProperties = applicationProperties
            Ticker.tickerRepository      = tickerRepository

            for(t in tickerRepository.findAll()) {
                tickers[t.symbol] = t
            }
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var tickerRepository: TickerRepository
        private val tickers: MutableMap<String, Ticker> = ConcurrentHashMap()

        fun saveIfNotExist(symbol: String) {
            if(tickerRepository.findById(symbol).isEmpty) {
                tickers[symbol] = save(Ticker(symbol = symbol))
            }
        }
        private fun save(ticker: Ticker) : Ticker {
            return tickerRepository.save(ticker)
        }
    }


    @Id
    @Column
    val symbol: String

    constructor(symbol: String) {
        this.symbol = symbol
    }
}