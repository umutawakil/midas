package com.midas.repositories

import com.midas.domain.Statistics
import org.springframework.data.repository.CrudRepository

interface StatisticsRepository: CrudRepository<Statistics, Long> {
}