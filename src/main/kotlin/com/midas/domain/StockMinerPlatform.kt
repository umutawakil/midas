package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.interfaces.IntraDayMarketWebService
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
 *
 * Platform for executing an algorithm continuously that is in sync with the market.
 * There's no mining algorithm in it directly but it calls on one of your choosing such as
 * PriceDeltaDetector, DeltaChain, etc.
 */
class StockMinerPlatform {
    @Component
    class SpringAdapter(
        @Autowired private val applicationProperties        : ApplicationProperties,
        @Autowired private val loggingService               : LoggingService
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

        fun downloadContinuously(
            intraDayMarketWebService: IntraDayMarketWebService
        ) {
            var runs = 0
            Thread.sleep(10000)
            while(runs != 4) {
                if (isMarketOpen()) {
                    /** Download snapshot data for the price change milestone ranker **/
                    downloadAndDetectDeltas(intraDayMarketWebService)
                    loggingService.log("Waiting....")
                    //Thread.sleep(60000)
                    Thread.sleep(60000*15)
                    runs++
                    if(runs==4) {
                        loggingService.log("Fourth run completed.")
                    }

                } else {
                    loggingService.log("Not currently the desired market hour. Waiting but will try again in ${applicationProperties.pollIntervalMins}")
                    Thread.sleep(1000L*60*applicationProperties.pollIntervalMins)
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

        private fun downloadAndDetectDeltas(intraDayMarketWebService: IntraDayMarketWebService) {
            executorService.execute {
                try {
                    loggingService.log("Requesting data for ranker...")
                    val date = Date(System.currentTimeMillis())
                    val jsonResult: JSONArray = intraDayMarketWebService.downloadRecords()["tickers"] as JSONArray
                    val records: MutableList<Pair<String, Double>> = mutableListOf()
                    for (i in jsonResult.indices) {
                        val prevDayObject = (jsonResult[i] as JSONObject)["prevDay"]
                        val todaysChangeObject = (jsonResult[i] as JSONObject)["todaysChange"]
                        if(prevDayObject == null ||todaysChangeObject == null) {
                            continue
                        }

                        val ticker = (jsonResult[i] as JSONObject)["ticker"] as String
                        val previousDayClose = Etl.double((prevDayObject as JSONObject)["c"])
                        val todaysChange     = Etl.double(todaysChangeObject)

                        val price = Etl.double(previousDayClose + todaysChange)
                        if(price <= 1.0) {
                            continue
                        }
                        records.add(
                            Pair(
                                first = ticker,
                                second = price
                            )
                        )
                    }
                    loggingService.log("Ranking data.....")
                    //PriceDeltaDetector.rank(date = date, stocks = records)
                    DeltaChain.addDeltas(date = date, stocks = records)

                } catch (ex: Exception) {
                    //TODO: Need to notify me the run failed
                    loggingService.log("Download failed for ranker. Sending push notification................")
                    loggingService.error(ex)
                }
            }
        }

        @Component
        private class PolyGonService : IntraDayMarketWebService {
            override fun downloadRecords() : JSONObject {
                val url: String = applicationProperties.polygonAllTickersURL +
                        "?apiKey=${applicationProperties.polyGonApiKey}&include_otc=true"
                return HttpUtility.getJSONObject(inputURL = url)
            }
        }
    }
}