package com.midas.jobs

import com.midas.configuration.ApplicationProperties
import com.midas.domain.IntraDayStockRecord
import com.midas.domain.Ticker
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


@Component
class MidasRunner(
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val intraDayStockRecord: IntraDayStockRecord
) {
    @PostConstruct
    fun init() {
        if(!applicationProperties.runIntraDayStockService) {
            println("Skipping intra-day stock records...")
        } else {
            println("Updating intra-day stock records.....")

            IntraDayStockRecord.downloadContinuously()

            println("Intra-day stock records updated.\r\n\r\n")
        }
    }
}