package com.midas.repositories

import com.midas.domain.UnsupportedTicker
import org.springframework.data.repository.CrudRepository

/**
 * Repository for Edgar tickers missing required data
 */

interface UnsupportedTickerRepository : CrudRepository<UnsupportedTicker, String> {
}