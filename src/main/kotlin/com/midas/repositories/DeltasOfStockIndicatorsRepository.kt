package com.midas.repositories

import com.midas.domain.DeltasOfStockIndicators
import com.midas.domain.IntraDayStockRecord
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface DeltasOfStockIndicatorsRepository : CrudRepository<DeltasOfStockIndicators, Long> {
    @Query("SELECT r FROM DeltasOfStockIndicators r WHERE r.creationDate = :creationDate ORDER BY r.creationDate Asc")
    fun findAllByCreationDate(creationDate: Date): List<DeltasOfStockIndicators>
}