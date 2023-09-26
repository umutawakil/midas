package com.midas.repositories

import com.midas.domain.StockSnapshot

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.*


interface IntraDayStockRecordRepository: CrudRepository<StockSnapshot, Long> {
    @Query("SELECT r FROM IntraDayStockRecordRepository r WHERE r.creationTime = :creationDate ORDER BY r.creationTime Asc")
    fun findAllByCreationTime(creationTime: Date): List<StockSnapshot>
}