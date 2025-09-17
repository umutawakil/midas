package com.midas.repositories

import com.midas.domain.Statistics
import org.springframework.data.repository.CrudRepository

/**
 * Repository for the Statistics
 */

interface StatisticsRepository: CrudRepository<Statistics, Long> {
}