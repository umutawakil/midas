package com.midas.repositories

import com.midas.domain.Delta
import org.springframework.data.repository.CrudRepository

interface DeltaRepository: CrudRepository<Delta, Long> {
}