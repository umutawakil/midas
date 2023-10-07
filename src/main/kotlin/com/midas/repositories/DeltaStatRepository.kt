package com.midas.repositories

import com.midas.domain.DeltaStat
import org.springframework.data.repository.CrudRepository

interface DeltaStatRepository: CrudRepository<DeltaStat, Long> {
}