package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.FinancialsRepository
import com.midas.repositories.IgnoreTickerRepository
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Entity
@Table(name="ignore_ticker")
class IgnoreTicker {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id   : Long = -1L
    private val name : String

    constructor(name: String) {
        this.name = name
    }

    @Component
    class SpringAdapter(
        @Autowired private val ignoreTickerRepository: IgnoreTickerRepository
    ) {
        @PostConstruct
        fun init() {
            IgnoreTicker.ignoreTickerRepository  = ignoreTickerRepository
        }
    }

    companion object {
        private lateinit var ignoreTickerRepository: IgnoreTickerRepository

        fun tickers() : Set<String> {
            return ignoreTickerRepository.findAll().map { it.name}.toSet()
        }

        fun save(name: String) {
            ignoreTickerRepository.save(
                IgnoreTicker(name = name)
            )
        }
    }
}