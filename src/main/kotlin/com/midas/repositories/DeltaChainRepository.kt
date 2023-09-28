package com.midas.repositories

import com.midas.domain.DeltaChain
import org.springframework.data.repository.CrudRepository

interface DeltaChainRepository : CrudRepository<DeltaChain, Long> {
}