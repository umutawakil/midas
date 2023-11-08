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
        @Autowired private val deltaSpringAdapter: Delta.SpringAdapter,
        @Autowired private val stockSnapshotSpringAdapter: StockSnapshot.SpringAdapter,
        @Autowired private val tickerSpringAdapter: Ticker.SpringAdapter,
        @Autowired private val milestoneSpringAdapter: Milestone.SpringAdapter,
        @Autowired private val financialsSpringAdapter: Financials.SpringAdapter
    ) {
        @PostConstruct
        fun init() {
            MidasRunner.applicationProperties    = applicationProperties
            MidasRunner.loggingService           = loggingService
            if (!Companion.applicationProperties.isNotIntegrationTest) {
                Companion.loggingService.log("MidasRunner skipped. This is expected if this is an integration test!")
                return
            }

            /** Ticker job---------------------------------------- **/
            //tickerSpringAdapter.init()


            /** Financials Job ------------------------------------**/
            /*financialsSpringAdapter.init()
            Financials.import()*/


            /** Historical StockSnapshot JOb--------------------**/
            /*stockSnapshotSpringAdapter.init()
            StockSnapshot.populatePastOneYearSnapshots()*/


            /** Milestone calculation job **/
            /*stockSnapshotSpringAdapter.init()
            StockSnapshot.calculateMilestones()*/
            

            /** Continuous stock snapshots Job **/
            /*deltaSpringAdapter.init()
            Companion.loggingService.log("Midas starting downloads. Lets get that MONEY!!!!!")
            Delta.continuouslyCalculateRealtimeDeltas()*/
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var loggingService: LoggingService
    }
}