package com.midas.domain

import com.midas.repositories.UnsupportedTickerRepository
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Entity
@Table(name="unsupported_ticker")
class UnsupportedTicker(
    @Id
    @Column(name="ticker")
    private val name: String
) {
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