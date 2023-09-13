package com.midas.repositories

import com.midas.domain.IntraDayStockRecord

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.util.*


interface IntraDayStockRecordRepository: CrudRepository<IntraDayStockRecord, Long> {
    //@Query("SELECT r FROM IntraDayStockRecordRepository r WHERE r.creationDate = :date")
    fun findAllByDate(
        @Param("date")date: Date
    ): Collection<IntraDayStockRecord>

}