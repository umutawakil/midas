package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.interfaces.IntraDayMarketWebService
import com.midas.repositories.IntraDayStockRecordRepository
import com.midas.services.LoggingService
import com.midas.utilities.Etl
import com.midas.utilities.HttpUtility
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Created by Usman Mutawakil on 2020-04-02.
 */
@Entity
@Table(name = "stock_snapshot")
class StockSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private val id: Long = -1

    @Column
    private val ticker: String
    @Column
    private val runNum: Int
    @Column
    private val price: Double
    @Column
    private val todaysChange: Double
    @Column
    private val creationTime: Date

    constructor(
            ticker: String,
            runNum: Int,
            price: Double,
            todaysChange: Double,
            creationDate: Date
    ) {
        this.ticker                 = ticker
        this.runNum                 = runNum
        this.price                  = price
        this.todaysChange           = todaysChange
        this.creationTime           = creationDate
    }

    @Component
    class SpringAdapter(
        @Autowired private val applicationProperties        : ApplicationProperties,
        @Autowired private val intraDayStockRecordRepository: IntraDayStockRecordRepository,
        @Autowired private val loggingService               : LoggingService
    ) {
        @PostConstruct
        fun init() {
            StockSnapshot.applicationProperties         = applicationProperties
            StockSnapshot.intraDayStockRecordRepository = intraDayStockRecordRepository
            StockSnapshot.loggingService                = loggingService

            loggingService.log("IntraDayStockRecord initialized")
        }
    }

    companion object {
        private lateinit var applicationProperties          : ApplicationProperties
        private lateinit var intraDayStockRecordRepository  : IntraDayStockRecordRepository
        private lateinit var loggingService                 : LoggingService
        private var          runNumber                      : Int = 0
        private val          executorService:ExecutorService      = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue<Runnable>()
        )
        private var          dailySnapshotTaken                   = false

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
                    if(applicationProperties.runStockSnapshotImport && !dailySnapshotTaken) {
                        try {
                            downloadAndImportStockSnapshots(
                                date = Date(System.currentTimeMillis()),
                                intraDayMarketWebService = intraDayMarketWebService
                            )

                        } catch (ex: Exception) {
                            //TODO: Need to notify me the run failed
                            loggingService.log("Download failed. Sending push notification................")
                            ex.printStackTrace()
                        }
                        dailySnapshotTaken = true
                        loggingService.log("Daily snapshot taken!")
                    }

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

        private fun downloadAndImportStockSnapshots(
            date                    : Date,
            intraDayMarketWebService: IntraDayMarketWebService
        ) {
            importStockSnapshots(
                    currentDate  = date,
                    stockRecords = intraDayMarketWebService.downloadRecords()["tickers"] as JSONArray
            )
        }

        private fun downloadAndDetectDeltas(intraDayMarketWebService: IntraDayMarketWebService) {
            executorService.execute {
                try {
                    loggingService.log("Requesting data for ranker...")
                    val date = Date(System.currentTimeMillis())
                    val jsonResult: JSONArray = intraDayMarketWebService.downloadRecords()["tickers"] as JSONArray
                    var records: MutableList<Pair<String, Double>> = mutableListOf()
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

        private fun importStockSnapshots(
                currentDate: Date,
                stockRecords: JSONArray
        ) {
            var errorFileWritten = false
            loggingService.log("Ingesting...")
            for(i in 0 until stockRecords.size) {
                val r = stockRecords[i] as JSONObject
                try {
                    if((r["prevDay"] == null ||(r["todaysChange"] == null))) {
                        continue
                    }

                    val previousDayClose = Etl.double((r["prevDay"] as JSONObject)["c"])
                    val todaysChange     = Etl.double((r["todaysChange"]))
                    val price            = Etl.double(previousDayClose + todaysChange)
                    if (price <= 0.0) {
                        continue
                    }

                    save(
                        StockSnapshot(
                            ticker       = r["ticker"] as String,
                            runNum       = runNumber,
                            price        = Etl.double((r["prevDay"] as JSONObject)["c"]) + Etl.double((r["todaysChange"])),
                            todaysChange = Etl.double((r["todaysChangePerc"])),
                            creationDate = currentDate
                        )
                    )

                } catch (ex: Exception) {
                    loggingService.log("Error processing stock record. See file for details -> \r\n")
                    ex.printStackTrace()

                    if(!errorFileWritten) {
                        writeDataToDiskForInspection(stockRecords = stockRecords)
                        errorFileWritten = true
                    }
                }
            }
            runNumber++
        }

        @Component
        private class PolyGonService : IntraDayMarketWebService {
            override fun downloadRecords() : JSONObject {
                val url: String = applicationProperties.polygonAllTickersURL +
                        "?apiKey=${applicationProperties.polyGonApiKey}&include_otc=true"
                return HttpUtility.getJSONObject(inputURL = url)
            }
        }

        private fun writeDataToDiskForInspection(stockRecords: JSONArray) {
            val formatter       = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val currentDateTime = LocalDateTime.now().format(formatter)
            val file            = File("${applicationProperties.errorDirectory}/intra-day-$currentDateTime.json")
            file.createNewFile()
            val printWriter = file.printWriter()
            printWriter.print(stockRecords.toJSONString())
            printWriter.flush()
            printWriter.close()
        }

        private fun save(record: StockSnapshot) : StockSnapshot {
            return intraDayStockRecordRepository.save(record)
        }

        fun deleteAll() {
            if(!applicationProperties.isNotIntegrationTest) {
                throw RuntimeException("data not deleted. This is not running in an integration environment")
            }
            intraDayStockRecordRepository.deleteAll()
        }
    }
}