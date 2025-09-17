package com.midas.domain

import com.midas.repositories.UnsupportedTickerRepository
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.Serializable

/**
 * Represents tickers in EDGAR data that are missing required information.
 */

@Entity
@Table(name="unsupported_ticker")
class UnsupportedTicker(
    @Id
    @Column(name="ticker")
    val name: String
) : Serializable {
    @Component
    private class SpringAdapter(
        @Autowired
        private val unsupportedTickerRepository: UnsupportedTickerRepository
    ) {
        @PostConstruct
        fun init() {
            UnsupportedTicker.unsupportedTickerRepository = unsupportedTickerRepository
        }
    }

    companion object {
        lateinit var unsupportedTickerRepository: UnsupportedTickerRepository

        fun save(u: UnsupportedTicker) {
            unsupportedTickerRepository.save(u)
        }
    }
}