package com.midas.repositories

import com.midas.domain.StockInfo
import org.springframework.data.repository.CrudRepository

/**
 * Repository for retrieving the core view info
 */

interface StockInfoRepository : CrudRepository<StockInfo, Long> {
}