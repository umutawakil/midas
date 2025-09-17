package com.midas.repositories

import com.midas.domain.Financials
import org.springframework.data.repository.CrudRepository

/**
 * The EDGAR data contains components that need to be ignored. This is different from ignored tickers
 */

interface SecIgnoredEntityRepository: CrudRepository<Financials.Companion.SecIgnoredEntity, Long> {
}