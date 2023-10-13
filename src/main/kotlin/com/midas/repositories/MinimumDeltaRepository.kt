package com.midas.repositories

import com.midas.domain.MinimumDelta
import org.springframework.data.repository.CrudRepository

interface MinimumDeltaRepository: CrudRepository<MinimumDelta, Long> {
}