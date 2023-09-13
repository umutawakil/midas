package com.midas.repositories

import com.midas.domain.StockIndicatorsDelta
import org.springframework.data.repository.CrudRepository

interface StockIndicatorsDeltaRepository : CrudRepository<StockIndicatorsDelta, Long> {
}