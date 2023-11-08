package com.midas.repositories

import com.midas.domain.IgnoreTicker
import org.springframework.data.repository.CrudRepository

interface IgnoreTickerRepository : CrudRepository<IgnoreTicker, Long> {
}