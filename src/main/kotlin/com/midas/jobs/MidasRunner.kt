package com.midas.jobs

import com.midas.configuration.ApplicationProperties
import com.midas.domain.Financials
import com.midas.domain.StockMinerPlatform
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
        @Autowired private val stockMinerPlatformSpringAdapter: StockMinerPlatform.SpringAdapter,
        @Autowired private val stockSnapshotSpringAdapter: StockSnapshot.SpringAdapter,
        @Autowired private val financialsSpringAdapter: Financials.SpringAdapter
    ) {
        @PostConstruct
        fun init() {
            MidasRunner.applicationProperties    = applicationProperties
            MidasRunner.loggingService           = loggingService
            MidasRunner.intraDayMarketWebService = intraDayMarketWebService

            if(!Companion.applicationProperties.isNotIntegrationTest) {
                Companion.loggingService.log("MidasRunner skipped. This is expected if this is an integration test!")
                return
            }

            /** PriceDelta Job **/
            /*if(!Companion.applicationProperties.runIntraDayStockService) {
                Companion.loggingService.log("Skipping intra-day stock records...")
                return
            } else {
                priceDeltaImporterSpringAdapter.init()
                Thread {
                    runPriceDeltaJob()
                }.start()
            }*/

            /**TODO: StockSnapshot JOb **/
            /*stockSnapshotSpringAdapter.init()
            StockSnapshot.populatePastOneYearSnapshots()*/

            /**TODO: Financials Job **/
            /*financialsSpringAdapter.init()
            Financials.populatePastOneYearFinancials()
            loggingService.log("MidasRunner initialized")*/
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var intraDayMarketWebService: IntraDayMarketWebService
        private lateinit var loggingService: LoggingService

        fun runPriceDeltaJob() {
            Thread.sleep(20000)
            try {
                loggingService.log("Midas starting downloads. Lets get that MONEY!!!!!")
                StockMinerPlatform.downloadContinuously(
                    intraDayMarketWebService = intraDayMarketWebService
                )
            } catch(ex: Exception) {
                loggingService.error(ex)
            }
        }
    }
}