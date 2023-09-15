package com.midas.jobs

import com.midas.configuration.ApplicationProperties
import com.midas.domain.IntraDayStockRecord
import com.midas.interfaces.IntraDayMarketWebService
import com.midas.interfaces.ExecutionWindowPicker
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*


@Component
class MidasRunner(
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val intraDayStockRecord: IntraDayStockRecord,
    @Autowired private val intraDayMarketWebService: IntraDayMarketWebService,
    @Autowired private val executionWindowPicker: ExecutionWindowPicker
) {
    @PostConstruct
    fun init() {
        if(applicationProperties.isIntegrationTest) {
            println("MidasRunner skipped. This is expected if this is an integration test!")
            return
        }
        if(!applicationProperties.runIntraDayStockService) {
            println("Skipping intra-day stock records...")
        } else {
            println("Updating intra-day stock records.....")

            IntraDayStockRecord.downloadContinuously(
                    date                     = Date(System.currentTimeMillis()),
                    intraDayMarketWebService = intraDayMarketWebService,
                    executionWindowPicker    = executionWindowPicker
            )

            println("Midas runner init has returned.\r\n\r\n")
        }
    }
}