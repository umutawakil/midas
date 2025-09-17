package com.midas.repositories

import com.midas.domain.StockSnapshot
import org.springframework.data.repository.CrudRepository

/**
 * Repository for the basic snapshot in time of any stock
 */
interface StockSnapshotRepository : CrudRepository<StockSnapshot, Long> {
}