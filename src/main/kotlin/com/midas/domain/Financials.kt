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
import java.util.*

@Entity
@Table(name="financials")
class Financials {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id                          : Long = -1L
    private val exchange                    : String
    private val sector                      : String
    private val industry                    : String
    private val ticker                      : String
    private val eps                         : Double
    private val marketCapitalization        : Double
    private val sharesOutstanding           : Long
    
    private val cashBurnPercentage          : Double
    private val equityBurnPercentage        : Double
    private val currentEquityBurnPercentage : Double
    private val assetDelta                  : Double
    private val liabilityDelta              : Double
    private val bookValueDelta              : Double

    //private val changeInCash                : Double
    private val cashOnHand                  : Double
    private val cashOnHandChange            : Double
    private val totalAssets                 : Double
    private val totalCurrentAssets          : Double
    private val totalLiabilities            : Double
    private val bookValue                   : Double
    private val lastReportDate              : Date

    constructor(
        exchange: String,
        ticker: String,
        sector: String,
        industry: String,
        eps: Double,
        marketCapitalization: Double,
        sharesOutstanding: Long,
        cashBurnPercentage: Double,
        equityBurnPercentage: Double,
        currentEquityBurnPercentage: Double,
        assetDelta: Double,
        liabilityDelta: Double,
        bookValueDelta: Double,
        cashOnHand: Double,
        cashOnHandChange: Double,
        totalAssets: Double,
        totalCurrentAssets: Double,
        totalLiabilities: Double,
        bookValue: Double,
        lastReportDate: Date
    ) {

        this.exchange                    = exchange
        this.ticker                      = ticker
        this.sector                      = sector
        this.industry                    = industry
        this.eps                         = eps
        this.marketCapitalization        = marketCapitalization
        this.sharesOutstanding           = sharesOutstanding
        this.cashBurnPercentage          = cashBurnPercentage
        this.equityBurnPercentage        = equityBurnPercentage
        this.currentEquityBurnPercentage = currentEquityBurnPercentage
        this.cashOnHand                  = cashOnHand
        this.cashOnHandChange            = cashOnHandChange
        this.assetDelta                  = assetDelta
        this.liabilityDelta              = liabilityDelta
        this.bookValueDelta              = bookValueDelta
        this.totalAssets                 = totalAssets
        this.totalCurrentAssets          = totalCurrentAssets
        this.totalLiabilities            = totalLiabilities
        this.bookValue                   = bookValue
        this.lastReportDate              = lastReportDate

    }
    @Component
    class SpringAdapter(
        @Autowired private val financialsRepository: FinancialsRepository,
        @Autowired private val tickerSpringAdapter: Ticker.SpringAdapter,
        @Autowired private val ignoreTickerSpringAdapter: IgnoreTicker.SpringAdapter,
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val loggingService: LoggingService
    ) {
        @PostConstruct
        fun init() {
            Financials.applicationProperties = applicationProperties
            Financials.financialsRepository  = financialsRepository
            Financials.loggingService        = loggingService

            tickerSpringAdapter.init()
            ignoreTickerSpringAdapter.init()
        }
    }

    companion object {
        private lateinit var financialsRepository: FinancialsRepository
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var loggingService: LoggingService
        private const val TIME_PER_REQUEST = 2000//50 // 50 ms per request, 20 per second, 1200 per minute
        fun import() {
            val completedTickers: Set<String> = financialsRepository.findAll().map { it.ticker}.toSet()
            val ignoreTickers: Set<String>    = IgnoreTicker.tickers()
            var start = System.currentTimeMillis()
            var count = 0

            loggingService.log("Ignore: " + ignoreTickers.size + ", completed: " + completedTickers.size)

            for (t in Ticker.getTickers()) {
                try {
                    if (!(completedTickers.contains(t) || ignoreTickers.contains(t))) {
                        val elapsedTime = System.currentTimeMillis() - start
                        if (elapsedTime >= TIME_PER_REQUEST) {
                            importForTicker(ticker = t)
                            start = System.currentTimeMillis()
                            count++
                            loggingService.log("count: $count, ticker: $t")

                        } else {
                            Thread.sleep(TIME_PER_REQUEST - elapsedTime)
                            importForTicker(ticker = t)
                            start = System.currentTimeMillis()
                            count++
                            loggingService.log("count: $count, ticker: $t")
                        }
                    }
                } catch(e: Exception) {
                    loggingService.log("Error: $t")
                    e.printStackTrace()
                    IgnoreTicker.save(t)
                }
            }
            loggingService.log("Financials imported for all relevant tickers.")
        }

        private fun importForTicker(ticker: String) {
            val o: JSONObject = sendRequestForTicker(ticker = ticker, function = "OVERVIEW")
            if (o.keys.size == 0) {
                println("$ticker not supported")
                IgnoreTicker.save(ticker)
                return
            }

            /** For ETFs, ETNs, and other non-stock securities these will return null on 'quarterlyReports'
             * Although this results in an error its' not a bug or real failure because those securities don't
             * actually have individualized financials.
             * **/
            //val cf: JSONArray  = sendRequestForTicker(ticker = ticker, function = "CASH_FLOW")["quarterlyReports"]        as JSONArray
            //val ics: JSONArray = sendRequestForTicker(ticker = ticker, function = "INCOME_STATEMENT")["quarterlyReports"] as JSONArray
            val bs: JSONArray  = sendRequestForTicker(ticker = ticker, function = "BALANCE_SHEET")["quarterlyReports"]    as JSONArray
            if (bs.size < 2) {
                println("Incorrect response sizes for $ticker for balance sheet: ${bs.size}")
                IgnoreTicker.save(ticker)
                return
            }
            val b: JSONObject = bs[0]  as JSONObject

            val totalAssets             = Etl.doubleS(b["totalAssets"])
            val totalCurrentAssets      = Etl.doubleS(b["totalCurrentAssets"])
            val totalLiabilities        = Etl.doubleS(b["totalLiabilities"])
            val totalCurrentLiabilities = Etl.doubleS(b["totalCurrentLiabilities"])
            val bookValue               = totalAssets - totalLiabilities
            val currentBookValue        = totalCurrentAssets - totalCurrentLiabilities
            val cashOnHandCurrent       = Etl.doubleS(b["cashAndShortTermInvestments"])
            var cashOnHandPrevious      = Etl.doubleS((bs[1] as JSONObject)["cashAndShortTermInvestments"])
            val cashOnHandChange        = cashOnHandCurrent - cashOnHandPrevious

            val assetCurrent = Etl.doubleS(b["totalAssets"])
            val assetPrevious = Etl.doubleS((bs[1] as JSONObject)["totalAssets"])
            val assetDelta = if(assetPrevious == 0.0) { 0.0 } else { (100 * (assetCurrent - assetPrevious))/assetPrevious}

            val liabilityCurrent = Etl.doubleS(b["totalLiabilities"])
            val liabilityPrevious = Etl.doubleS((bs[1] as JSONObject)["totalLiabilities"])
            val liabilityDelta = if(liabilityPrevious == 0.0) { 0.0 } else { (100 * (liabilityCurrent - liabilityPrevious))/liabilityPrevious}

            val bookValuePrevious = Etl.doubleS((bs[1] as JSONObject)["totalAssets"]) - Etl.doubleS((bs[1] as JSONObject)["totalLiabilities"])
            val bookValueDelta = if(bookValuePrevious == 0.0) { 0.0 } else { (100*(bookValue - bookValuePrevious)) / bookValuePrevious }
            /** TODO: Where really should this check go if anywhere? **/
            if (bookValue == 0.0) {
                IgnoreTicker.save(ticker)
                return
            }

            financialsRepository.save(
                Financials(
                    exchange                    = o["Exchange"] as String,
                    sector                      = o["Sector"] as String,
                    industry                    = o["Industry"] as String,
                    ticker                      = ticker,
                    eps                         = Etl.doubleS(o["EPS"]),
                    marketCapitalization        = Etl.doubleS(o["MarketCapitalization"]),
                    sharesOutstanding           = (o["SharesOutstanding"] as String).toLong(),
                    cashBurnPercentage          = if(cashOnHandPrevious == 0.0) { 0.0 } else {(-100.0 * cashOnHandChange) / cashOnHandPrevious },
                    equityBurnPercentage        = (-100.0 * cashOnHandChange) / bookValue,
                    currentEquityBurnPercentage = if(currentBookValue == 0.0) { 0.0 } else {(-100.0 * cashOnHandChange) / currentBookValue },
                    cashOnHand                  = cashOnHandCurrent,
                    cashOnHandChange            = cashOnHandChange,
                    assetDelta                  = assetDelta,
                    liabilityDelta              = liabilityDelta,
                    bookValueDelta              = bookValueDelta,
                    totalAssets                 = totalAssets,
                    totalCurrentAssets          = totalCurrentAssets,
                    totalLiabilities            = totalLiabilities,
                    bookValue                   = bookValue,
                    lastReportDate              = SimpleDateFormat("yyyy-MM-dd").parse(b["fiscalDateEnding"] as String)
                )
            )
        }

        private fun sendRequestForTicker(ticker: String, function: String) : JSONObject {
            val url: String = "${applicationProperties.financialsApiUrl}/query?function=$function&symbol=$ticker&apikey="+
                    "${applicationProperties.financialsApiKey}"

            return HttpUtility.getJSONObject(inputURL = url)
        }
    }
}