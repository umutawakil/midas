package com.midas.repositories

import com.midas.domain.StockSnapshot
import org.springframework.data.repository.CrudRepository

interface StockSnapshotRepository : CrudRepository<StockSnapshot, Long> {
}