package com.midas.repositories

import com.midas.domain.Financials
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface FinancialsRepository : CrudRepository<Financials, Long> {
    //@Modifying
    //@Query("DELETE f FROM Financials f")
    //fun deleteEvery()
}