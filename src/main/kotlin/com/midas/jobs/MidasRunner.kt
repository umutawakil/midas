package com.midas.jobs

import com.midas.configuration.ApplicationProperties
import com.midas.domain.IntraDayStockRecord
import com.midas.interfaces.IntraDayMarketWebService
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MidasRunner(
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val intraDayMarketWebService: IntraDayMarketWebService,
    @Autowired private val intraDayStockRecord: IntraDayStockRecord.SpringAdapter,
    @Autowired private val loggingService: LoggingService
) {
    @PostConstruct
    fun init() {
        //loggingService.log("Waiting for beans to initialize....")
        //Thread.sleep(1000*30)
        if(!applicationProperties.isNotIntegrationTest) {
            loggingService.log("MidasRunner skipped. This is expected if this is an integration test!")
            return
        }
        if(!applicationProperties.runIntraDayStockService) {
            loggingService.log("Skipping intra-day stock records...")
        } else {
            loggingService.log("Updating intra-day stock records (9).....")
            IntraDayStockRecord.downloadContinuously(
                intraDayMarketWebService = intraDayMarketWebService
            )
        }
    }
}