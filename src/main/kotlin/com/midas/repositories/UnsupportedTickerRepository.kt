package com.midas.repositories

import com.midas.domain.UnsupportedTicker
import org.springframework.data.repository.CrudRepository

interface UnsupportedTickerRepository : CrudRepository<UnsupportedTicker, String> {
}