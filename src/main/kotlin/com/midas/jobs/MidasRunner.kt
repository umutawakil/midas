package com.midas.jobs

import com.midas.configuration.ApplicationProperties
import com.midas.domain.IntraDayStockRecord
import com.midas.interfaces.IntraDayMarketWebService
import com.midas.repositories.IntraDayStockRecordRepository
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

class MidasRunner {
    @Component
    class SpringAdapter(
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val intraDayMarketWebService: IntraDayMarketWebService,
        @Autowired private val loggingService: LoggingService
    ) {
        @PostConstruct
        fun init() {
            MidasRunner.applicationProperties    = applicationProperties
            MidasRunner.loggingService           = loggingService
            MidasRunner.intraDayMarketWebService = intraDayMarketWebService
            loggingService.log("MidasRunner initialized")

            launch()
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var intraDayMarketWebService: IntraDayMarketWebService
        private lateinit var loggingService: LoggingService
        fun launch() {
            if(!applicationProperties.isNotIntegrationTest) {
                loggingService.log("MidasRunner skipped. This is expected if this is an integration test!")
                return
            }
            if(!applicationProperties.runIntraDayStockService) {
                loggingService.log("Skipping intra-day stock records...")
            } else {
                try {
                    loggingService.log("Midas starting downloads. Lets get that MONEY!!!!!")
                    IntraDayStockRecord.downloadContinuously(
                        intraDayMarketWebService = intraDayMarketWebService
                    )
                } catch(ex: Exception) {
                    loggingService.error(ex)
                }
            }
        }
    }
}