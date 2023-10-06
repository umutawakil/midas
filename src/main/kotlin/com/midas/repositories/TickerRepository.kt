package com.midas.repositories

import com.midas.domain.Ticker
import org.springframework.data.repository.CrudRepository

interface TickerRepository: CrudRepository<Ticker, Long> {
}