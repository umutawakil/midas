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
        @Autowired private val stockSnapshotSpringAdapter: StockSnapshot.SpringAdapter,
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

            /** TODO: There are various ways to do this without hardcoding the class. Can identify the class from a bean id etc **/
            val jobName = System.getenv("jobName")

            /** Financials Job ----------------------------------------------------**/
            if (jobName == "import-financials") {
                financialsSpringAdapter.init()
                Financials.import()
                return
            }

            /** Stock snapshot import job -----------------------------------------**/
            if (jobName == "import-snapshots") {
                stockSnapshotSpringAdapter.init()
                StockSnapshot.populatePastOneYearSnapshots()
                return
            }

            /** Calculate stock statistics job -------------------------------------**/
            if (jobName == "calculate-statistics") {
                stockSnapshotSpringAdapter.init()
                StockSnapshot.calculateStatistics()
                return
            }
            throw RuntimeException("Job not found by name $jobName")
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var loggingService: LoggingService
    }
}