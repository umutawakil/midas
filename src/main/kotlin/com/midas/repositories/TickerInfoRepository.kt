package com.midas.repositories

import com.midas.domain.TickerInfo
import org.springframework.data.repository.CrudRepository

/**
 * Repository for general ticker info
 */
interface TickerInfoRepository : CrudRepository<TickerInfo, String> {
}