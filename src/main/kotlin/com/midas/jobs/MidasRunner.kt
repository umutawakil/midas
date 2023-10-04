package com.midas.jobs

import com.midas.configuration.ApplicationProperties
import com.midas.domain.StockSnapshot
import com.midas.interfaces.IntraDayMarketWebService
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

class MidasRunner {
    @Component
    class SpringAdapter(
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val intraDayMarketWebService: IntraDayMarketWebService,
        @Autowired private val loggingService: LoggingService,
        @Autowired private val stockSnapshotSpringAdapter: StockSnapshot.SpringAdapter,
    ) {
        @PostConstruct
        fun init() {
            if(!Companion.applicationProperties.runIntraDayStockService) {
                Companion.loggingService.log("Skipping intra-day stock records...")
                return
            }
            stockSnapshotSpringAdapter.init()
            MidasRunner.applicationProperties    = applicationProperties
            MidasRunner.loggingService           = loggingService
            MidasRunner.intraDayMarketWebService = intraDayMarketWebService

            loggingService.log("MidasRunner initialized")
            if(!Companion.applicationProperties.isNotIntegrationTest) {
                Companion.loggingService.log("MidasRunner skipped. This is expected if this is an integration test!")
                return
            }

            Thread {
                launch()
            }.start()
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var intraDayMarketWebService: IntraDayMarketWebService
        private lateinit var loggingService: LoggingService
        fun launch() {
            Thread.sleep(20000)
            if(!applicationProperties.isNotIntegrationTest) {
                loggingService.log("MidasRunner skipped. This is expected if this is an integration test!")
                return
            }
            try {
                loggingService.log("Midas starting downloads. Lets get that MONEY!!!!!")
                StockSnapshot.downloadContinuously(
                    intraDayMarketWebService = intraDayMarketWebService
                )
            } catch(ex: Exception) {
                loggingService.error(ex)
            }
        }
    }
}