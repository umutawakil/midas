package com.midas.repositories

import com.midas.domain.IgnoreTicker
import org.springframework.data.repository.CrudRepository

/**
 * There are unfortunately tickers that correspond to some sort of placeholder data on the
 * side of our data suppliers and need to be ignored.
 */

interface IgnoreTickerRepository : CrudRepository<IgnoreTicker, Long> {
}