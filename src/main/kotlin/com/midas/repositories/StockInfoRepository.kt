package com.midas.repositories

import com.midas.domain.StockInfo
import org.springframework.data.repository.CrudRepository

interface StockInfoRepository : CrudRepository<StockInfo, Long> {
}