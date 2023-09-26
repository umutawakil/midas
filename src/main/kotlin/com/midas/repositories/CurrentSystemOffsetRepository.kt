package com.midas.repositories

import com.midas.domain.PriceDeltaDetector
import org.springframework.data.repository.CrudRepository

interface CurrentSystemOffsetRepository : CrudRepository<PriceDeltaDetector.CurrentSystemOffset, Long> {
}