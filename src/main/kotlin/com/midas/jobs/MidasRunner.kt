package com.midas.jobs

import com.midas.configuration.ApplicationProperties
import com.midas.domain.*
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
        @Autowired private val loggingService: LoggingService,
        @Autowired private val stockMinerPlatformSpringAdapter: StockMinerPlatform.SpringAdapter,
        @Autowired private val stockSnapshotSpringAdapter: StockSnapshot.SpringAdapter,
        @Autowired private val financialsSpringAdapter: Financials.SpringAdapter,
        @Autowired private val minimumDeltaSpringAdapter: MinimumDelta.SpringAdapter,
        @Autowired private val tickerSpringAdapter: Ticker.SpringAdapter,
        @Autowired private val milestoneSpringAdapter: Milestone.SpringAdapter
    ) {
        @PostConstruct
        fun init() {
            MidasRunner.applicationProperties    = applicationProperties
            MidasRunner.loggingService           = loggingService
            if (!Companion.applicationProperties.isNotIntegrationTest) {
                Companion.loggingService.log("MidasRunner skipped. This is expected if this is an integration test!")
                return
            }

            /** Financials Job------------------------------------- **/
            /*financialsSpringAdapter.init()
            Financials.populatePastOneYearFinancials()
            loggingService.log("MidasRunner initialized")*/


            /** Historical StockSnapshot JOb 1 --------------------**/
            /*stockSnapshotSpringAdapter.init()
            StockSnapshot.populatePastOneYearSnapshots()*/

            /** Minimum delta job **/
            /*minimumDeltaSpringAdapter
            MinimumDelta.calculate()*/
            
            /** Ticker job---------------------------------------- **/
            //tickerSpringAdapter.init()

            /** Historical StockSnapshot JOb 2**/
            /*stockSnapshotSpringAdapter.init()
            StockSnapshot.populatePastOneMonthSnapshots()*/

            /** Milestone calculation job **/
            /*milestoneSpringAdapter.init()
            Milestone.calculateMilestones()*/

            /** Continuous stock snapshots Job **/
            stockMinerPlatformSpringAdapter.init()
            Companion.loggingService.log("Midas starting downloads. Lets get that MONEY!!!!!")
            StockMinerPlatform.recordRealTimeMarketChangesContinuously()
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var loggingService: LoggingService
    }
}