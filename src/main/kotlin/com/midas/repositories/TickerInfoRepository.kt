package com.midas.repositories

import com.midas.domain.TickerInfo
import org.springframework.data.repository.CrudRepository

interface TickerInfoRepository : CrudRepository<TickerInfo, String> {
}