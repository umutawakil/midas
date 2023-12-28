package com.midas.repositories

import com.midas.domain.Financials
import org.springframework.data.repository.CrudRepository

interface SecIgnoredEntityRepository: CrudRepository<Financials.Companion.SecIgnoredEntity, Long> {
}