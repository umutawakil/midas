package com.midas.jobs

import com.midas.configuration.ApplicationProperties
import com.midas.domain.*
import com.midas.interfaces.IntraDayMarketWebService
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * This is just a hook from where to call the process/job you actually want to run.
 */
class MidasRunner {
    @Component
    class SpringAdapter(
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val intraDayMarketWebService: IntraDayMarketWebService,
        @Autowired private val loggingService: LoggingService,
        @Autowired private val stockMinerPlatformSpringAdapter: StockMinerPlatform.SpringAdapter,
        @Autowired private val stockSnapshotSpringAdapter: StockSnapshot.SpringAdapter,
        @Autowired private val financialsSpringAdapter: Financials.SpringAdapter,
        @Autowired private val deltaStatSpringAdapter: DeltaStat.SpringAdapter,
        @Autowired private val minimumDeltaSpringAdapter: MinimumDelta.SpringAdapter
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
            stockMinerPlatformSpringAdapter.init()
            Companion.loggingService.log("Midas starting downloads. Lets get that MONEY!!!!!")
            StockMinerPlatform.downloadContinuously(
                intraDayMarketWebService = Companion.intraDayMarketWebService
            )

            /**TODO: StockSnapshot JOb **/
            /*stockSnapshotSpringAdapter.init()
            StockSnapshot.populatePastOneYearSnapshots()*/

            /**TODO: Financials Job **/
            /*financialsSpringAdapter.init()
            Financials.populatePastOneYearFinancials()
            loggingService.log("MidasRunner initialized")*/

            /**TODO: DeltaStat Job **/
            /*deltaStatSpringAdapter.init()
            DeltaStat.calculateDeltaStats()*/

            /** TODO: Minimum delta job **/
            /*minimumDeltaSpringAdapter
            MinimumDelta.calculate()*/
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var intraDayMarketWebService: IntraDayMarketWebService
        private lateinit var loggingService: LoggingService
    }
}