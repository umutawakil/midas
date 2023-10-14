package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.services.LoggingService
import com.midas.utilities.Etl
import com.midas.utilities.HttpUtility
import jakarta.annotation.PostConstruct
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Platform for polling the market for price updates and executing a strategy/callback against each
 * result for each ticker. This is autility class for other class that want to continuously process real-time
 * market data.
 */
class StockMinerPlatform {
    @Component
    class SpringAdapter(
        @Autowired private val applicationProperties : ApplicationProperties,
        @Autowired private val loggingService        : LoggingService
    ) {
        @PostConstruct
        fun init() {
            StockMinerPlatform.applicationProperties = applicationProperties
            StockMinerPlatform.loggingService        = loggingService

            loggingService.log("IntraDayStockRecord initialized")
        }
    }

    companion object {
        private lateinit var applicationProperties          : ApplicationProperties
        private lateinit var loggingService                 : LoggingService
        private val          executorService:ExecutorService = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue()
        )

        fun recordRealTimeMarketChangesContinuously(priceUpdateHandlerStrategy: PriceUpdateHandlerStrategy) {
            while (true) {
                if (isMarketOpen()) {
                    download(priceUpdateHandlerStrategy)
                    loggingService.log("Waiting....")
                    Thread.sleep(60000 * 15)

                } else {
                    loggingService.log("Not currently the desired market hour. Waiting but will try again in ${applicationProperties.pollIntervalMins}")
                    Thread.sleep(1000L * 60 * applicationProperties.pollIntervalMins)
                }
            }
        }

        private fun isMarketOpen() : Boolean {
            val calendar = Calendar.getInstance()
            calendar.timeZone = TimeZone.getTimeZone("America/New_York")
            val dayOfTheWeek: Int = calendar[Calendar.DAY_OF_WEEK]
            if (dayOfTheWeek == 1 || dayOfTheWeek == 7) {
                loggingService.log("skipping for the weekend. Day number: $dayOfTheWeek")
               return false
            }
            val hour    = calendar[Calendar.HOUR_OF_DAY]
            val minutes = calendar[Calendar.MINUTE]
            loggingService.log("T:$hour:$minutes")

            //return (hour >= 4) && (hour < 16)
            if(hour==9 && minutes < 30) {
                return false
            }
            if(hour==11 && minutes >= 30) {
                return false
            }
            return (hour >= 9) && (hour <= 11)
        }

        private fun download(priceUpdateHandlerStrategy: PriceUpdateHandlerStrategy) {
            executorService.execute {
                try {
                    loggingService.log("Requesting data for ranker...")
                    val jsonResult: JSONArray = downloadRecords()["tickers"] as JSONArray
                    val previousPrices: MutableMap<String, Double> = HashMap()

                    for (i in jsonResult.indices) {
                        val prevDayObject          = (jsonResult[i] as JSONObject)["prevDay"]
                        val dayObject              = (jsonResult[i] as JSONObject)["day"]
                        val todaysChangeObject     = (jsonResult[i] as JSONObject)["todaysChange"]
                        val todaysChangePercObject = (jsonResult[i] as JSONObject)["todaysChangePerc"]
                        if(prevDayObject == null ||todaysChangeObject == null) {
                            continue
                        }

                        val ticker = (jsonResult[i] as JSONObject)["ticker"] as String
                        val previousDayClose = Etl.double((prevDayObject as JSONObject)["c"])
                        val openPrice        = Etl.double((dayObject as JSONObject)["o"])
                        val todaysChange     = Etl.double(todaysChangeObject)
                        val todaysChangePerc = Etl.double(todaysChangePercObject)

                        val price = Etl.double(previousDayClose + todaysChange)
                        if(price <= 0.1) {
                            continue
                        }
                        val previousPrice: Double? = previousPrices[ticker]
                        val runningDelta: Double   = if(previousPrice == null) { 0.0 } else {100.0*((price - previousPrice) / previousPrice) }
                        previousPrices[ticker]     = price

                        /*Delta.save(
                            Delta(
                                ticker             = ticker,
                                price              = price,
                                delta              = todaysChangePerc,
                                runningDelta       = runningDelta,
                                previousClosePrice = previousDayClose,
                                openPrice          = openPrice
                            )
                        )*/
                        priceUpdateHandlerStrategy.process(
                            ticker             = ticker,
                            price              = price,
                            delta              = todaysChangePerc,
                            runningDelta       = runningDelta,
                            previousClosePrice = previousDayClose,
                            openPrice          = openPrice
                        )
                    }
                    loggingService.log("Ranking data.....")

                } catch (ex: Exception) {
                    //TODO: Need to notify me the run failed
                    loggingService.log("Download failed for ranker. Sending push notification................")
                    loggingService.error(ex)
                }
            }
        }

        interface PriceUpdateHandlerStrategy {
            fun process(
                ticker: String,
                price: Double,
                delta: Double,
                runningDelta: Double,
                previousClosePrice: Double,
                openPrice: Double
            )
        }

        private fun downloadRecords() : JSONObject {
            val url: String = applicationProperties.polygonAllTickersURL +
                    "?apiKey=${applicationProperties.polyGonApiKey}&include_otc=true"
            return HttpUtility.getJSONObject(inputURL = url)
        }

    }
}