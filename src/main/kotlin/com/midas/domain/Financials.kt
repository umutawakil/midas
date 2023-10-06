package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.FinancialsRepository
import com.midas.services.LoggingService
import com.midas.utilities.Etl
import com.midas.utilities.HttpUtility
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


//TODO: /vX/reference/financials
@Entity
@Table(name="financials")
class Financials {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id                : Long = -1L
    private val ticker            : String
    private val assets            : Double
    private val liabilities       : Double
    private val bookValue         : Double
    private val netCashFlow       : Double
    private val operatingCashFlow : Double
    private val grossProfit       : Double
    private val revenue           : Double
    private val startDate         : Date
    private val endDate           : Date

    constructor(
        ticker: String,
        assets: Double,
        liabilities: Double,
        netCashFlow: Double,
        operatingCashFlow: Double,
        grossProfit: Double,
        revenue: Double,
        startDate: Date,
        endDate: Date
    )
    {
        this.ticker            = ticker
        this.assets            = assets
        this.liabilities       = liabilities
        this.bookValue         = assets - liabilities
        this.netCashFlow       = netCashFlow
        this.operatingCashFlow = operatingCashFlow
        this.grossProfit       = grossProfit
        this.revenue           = revenue
        this.startDate         = startDate
        this.endDate           = endDate
    }

    @Component
    class SpringAdapter(
        @Autowired val applicationProperties  : ApplicationProperties,
        @Autowired val financialsRepository   : FinancialsRepository,
        @Autowired val loggingService         : LoggingService,
        @Autowired val tickerSpringAdapter    : Ticker.SpringAdapter
    ) {
        @PostConstruct
        fun init() {
            Financials.applicationProperties = applicationProperties
            Financials.financialsRepository  = financialsRepository
            Financials.loggingService        = loggingService

            tickerSpringAdapter.init()
        }
    }

    companion object {
        lateinit var financialsRepository : FinancialsRepository
        lateinit var loggingService       : LoggingService
        lateinit var applicationProperties: ApplicationProperties
        private val  executorService: ExecutorService = ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue()
        )
        @Volatile
        private var processedCount = 0

        fun populatePastOneYearFinancials() {
            val periodOfReportDate = "2023-01-01"
            for(t:String in Ticker.getTickers()) {
                getFinancials(
                    ticker             = t,
                    periodOfReportDate = periodOfReportDate
                )
            }
            //TODO: What difference are the threads making here if any at all?
        }

        private fun getFinancials(ticker: String, periodOfReportDate: String) {
            executorService.execute {
                getFinancialsWorker(ticker = ticker, periodOfReportDate = periodOfReportDate)
            }
        }

        private fun getFinancialsWorker(ticker: String, periodOfReportDate: String) {
            val url: String = "${applicationProperties.polygonBaseUrl}/vX/reference/financials?period_of_report_date.gte=$periodOfReportDate"+
                    "&ticker=$ticker&apiKey=${applicationProperties.polyGonApiKey}"
            val result: JSONObject = HttpUtility.getJSONObject(inputURL = url)

            //val count: Long = result["count"] as Long
            //if(count == 0L) return

            val dateFormatter = SimpleDateFormat("yyyy-MM-dd")

            val results = result["results"] as JSONArray
            for(i in results.indices) {
                loggingService.log("Ticker: $ticker")
                val startDate: Date                = dateFormatter.parse((results[i] as JSONObject)["start_date"] as String)
                val endDate: Date                  = dateFormatter.parse((results[i] as JSONObject)["end_date"] as String)
                val financialsObject: JSONObject   = (results[i] as JSONObject)["financials"] as JSONObject
                val balanceSheet: JSONObject?      = financialsObject["balance_sheet"] as JSONObject?
                val cashFlowStatement: JSONObject? = financialsObject["cash_flow_statement"] as JSONObject?
                val incomeStatement: JSONObject?   = financialsObject["income_statement"] as JSONObject?
                if(balanceSheet == null || cashFlowStatement == null || incomeStatement == null) {
                    continue
                }

                val assets: Double?             = Etl.doubleN((balanceSheet["assets"] as JSONObject?)?.get("value"))
                val liabilities: Double?        = Etl.doubleN((balanceSheet["liabilities"] as JSONObject?)?.get("value"))
                val netCashFlow: Double?        = Etl.doubleN((cashFlowStatement["net_cash_flow"] as JSONObject?)?.get("value"))
                val operatingCashFlow: Double?  = Etl.doubleN((cashFlowStatement["net_cash_flow_from_operating_activities"] as JSONObject?)?.get("value"))
                val grossProfit: Double?        = Etl.doubleN((incomeStatement["gross_profit"] as JSONObject?)?.get("value"))
                val revenue: Double?            = Etl.doubleN((incomeStatement["revenues"]  as JSONObject?)?.get("value"))

                if(
                    netCashFlow == null ||
                    operatingCashFlow == null ||
                    grossProfit == null ||
                    revenue == null ||
                    assets == null ||
                    liabilities == null
                    ) {
                    continue
                }

                financialsRepository.save(
                    Financials(
                        ticker            = ticker,
                        assets            = assets,
                        liabilities       = liabilities,
                        netCashFlow       = netCashFlow,
                        operatingCashFlow = operatingCashFlow,
                        grossProfit       = grossProfit,
                        revenue           = revenue,
                        startDate         = startDate,
                        endDate           = endDate
                    )
                )
            }
            processedCount++
            loggingService.log("Count: $processedCount")
        }
    }
}