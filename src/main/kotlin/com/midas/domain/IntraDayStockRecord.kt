package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.interfaces.IntraDayMarketWebService
import com.midas.repositories.IntraDayStockRecordRepository
import com.midas.services.LoggingService
import com.midas.utilities.DomainValueCompareUtil
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

/**
 * Created by Usman Mutawakil on 2020-04-02.
 */
@Component
@Entity
@Table(name = "intra_day_stock_record")
class IntraDayStockRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private val id: Long = -1

    @Column
    private val ticker: String
    @Column
    private val price: Double
    @Column
    private val vwap: Double
    @Column
    private val openPrice: Double
    @Column
    private val lowPrice: Double
    @Column
    private val highPrice: Double
    @Column
    private val accumulatedVolume: Double
    @Column
    private val todaysChange: Double
    @Column
    private val todaysChangePercentage: Double
    @Column
    private val previousDayVolume: Double
    @Column
    private val previousDayLow: Double
    @Column
    private val previousDayHigh: Double
    @Column
    private val previousDayOpen: Double
    @Column
    private val previousDayClose: Double
    @Column
    private val priceDelta: Double
    @Column
    private val increasing: Boolean
    @Column
    private val timeDiffMins: Int
    @Column
    private val externalTime: Long
    @Column
    private val creationDate: Date

    constructor(
            ticker: String,
            price: Double,
            vwap: Double,
            openPrice: Double,
            lowPrice: Double,
            highPrice: Double,
            accumulatedVolume: Double,
            todaysChange: Double,
            todaysChangePercentage: Double,
            previousDayVolume: Double,
            previousDayClose: Double,
            previousDayOpen: Double,
            previousDayHigh: Double,
            previousDayLow: Double,
            priceDelta: Double,
            increasing: Boolean,
            timeDiffMins: Int,
            externalTime: Long,
            creationDate: Date
    ) {
        this.ticker                 = ticker
        this.price                  = price
        this.vwap                   = vwap
        this.openPrice              = openPrice
        this.lowPrice               = lowPrice
        this.highPrice              = highPrice
        this.accumulatedVolume      = accumulatedVolume
        this.todaysChange           = todaysChange
        this.todaysChangePercentage = todaysChangePercentage
        this.previousDayVolume      = previousDayVolume
        this.previousDayClose       = previousDayClose
        this.previousDayOpen        = previousDayOpen
        this.previousDayHigh        = previousDayHigh
        this.previousDayLow         = previousDayLow
        this.priceDelta             = priceDelta
        this.increasing             = increasing
        this.timeDiffMins           = timeDiffMins
        this.externalTime           = externalTime
        this.creationDate           = creationDate
    }

    @Component
    class SpringAdapter(
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val intraDayStockRecordRepository: IntraDayStockRecordRepository,
        @Autowired private val loggingService: LoggingService
    ) {
        @PostConstruct
        fun init() {
            IntraDayStockRecord.applicationProperties         = applicationProperties
            IntraDayStockRecord.intraDayStockRecordRepository = intraDayStockRecordRepository
            IntraDayStockRecord.loggingService                = loggingService

            loggingService.log("IntraDayStockRecord initialized")
        }
    }

    companion object {
        private lateinit var applicationProperties          : ApplicationProperties
        private lateinit var intraDayStockRecordRepository  : IntraDayStockRecordRepository
        private lateinit var loggingService                 : LoggingService
        private val          stockMap: MutableMap<String, IntraDayStockRecord> = HashMap()

        fun downloadContinuously(
            intraDayMarketWebService: IntraDayMarketWebService
        ) {
            while(true) {
                if (isMarketOpen()) {
                    val start = System.currentTimeMillis()
                    loggingService.log("Market is open")
                    try {
                        download(
                            date = Date(System.currentTimeMillis()),
                            intraDayMarketWebService = intraDayMarketWebService
                        )

                    } catch (ex: Exception) {
                        //TODO: Need to notify me the run failed
                        loggingService.log("Download failed. Sending push notification................")
                        ex.printStackTrace()
                    }
                    val timeDiff = (System.currentTimeMillis() - start)
                    loggingService.log("Ingestion time: " + ((timeDiff/1000)/60))
                    loggingService.log("Waiting...")

                    Thread.sleep((1000L * 60 * applicationProperties.pollIntervalMins) - timeDiff)
                    loggingService.log("Wait time: " + (((System.currentTimeMillis() - start)/1000L)/60))


                } else {
                    loggingService.log("Market is closed")
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

            return (hour >= 4) && (hour < 16)
        }

        private fun download(
            date                    : Date,
            intraDayMarketWebService: IntraDayMarketWebService
        ) {
            ingest(
                    currentDate     = date,
                    stockRecords    = intraDayMarketWebService.downloadRecords()["tickers"] as JSONArray
            )
        }
        private fun ingest(
                currentDate: Date,
                stockRecords: JSONArray
        ) {
            var errorFileWritten = false
            loggingService.log("Ingesting...")
            for(i in 0 until stockRecords.size) {
                val r = stockRecords[i] as JSONObject
                try {
                    val previousDayClose  = Etl.double((r["prevDay"] as JSONObject)["c"])
                    val todaysChange      = Etl.double((r["todaysChange"]))
                    val ticker            = r["ticker"] as String
                    val price             = Etl.double(previousDayClose + todaysChange)

                    val previousRecord: IntraDayStockRecord? = stockMap[ticker]
                    var increasing = false
                    var priceDelta = 0.0
                    var timeDiffMins = 0
                    if(previousRecord != null) {
                        if(price > previousRecord.price) {
                            increasing = true
                        }
                        if(previousRecord.price != 0.0) {
                            priceDelta = ((price - previousRecord.price) / previousRecord.price) * 100.0
                        }
                        timeDiffMins = (((currentDate.time - previousRecord.creationDate.time) / 1000) / 60).toInt()
                    }

                    val newRecord = save(
                        IntraDayStockRecord(
                            ticker                 = ticker,
                            price                  = price,
                            vwap                   = Etl.double((r["min"] as JSONObject)["vw"]),
                            openPrice              = Etl.double((r["day"] as JSONObject)["o"]),
                            highPrice              = Etl.double((r["day"] as JSONObject)["h"]),
                            lowPrice               = Etl.double((r["day"] as JSONObject)["l"]),
                            accumulatedVolume      = Etl.double((r["min"] as JSONObject)["av"]),
                            todaysChange           = todaysChange,
                            todaysChangePercentage = Etl.double((r["todaysChangePerc"])),
                            previousDayVolume      = Etl.double((r["prevDay"] as JSONObject)["v"]),
                            previousDayClose       = previousDayClose,
                            previousDayOpen        = Etl.double((r["prevDay"] as JSONObject)["o"]),
                            previousDayHigh        = Etl.double((r["prevDay"] as JSONObject)["h"]),
                            previousDayLow         = Etl.double((r["prevDay"] as JSONObject)["l"]),
                            increasing             = increasing,
                            priceDelta             = priceDelta,
                            timeDiffMins           = timeDiffMins,
                            externalTime           = (r["updated"] as Long),
                            creationDate           = currentDate
                        )
                    )
                    stockMap[ticker] = newRecord

                } catch (ex: Exception) {
                    loggingService.log("Error processing stock record. See file for details -> \r\n")
                    ex.printStackTrace()

                    if(!errorFileWritten) {
                        writeDataToDiskForInspection(stockRecords = stockRecords)
                        errorFileWritten = true
                    }
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

        private fun save(record: IntraDayStockRecord) : IntraDayStockRecord {
            return intraDayStockRecordRepository.save(record)
        }

        fun deleteAll() {
            if(!applicationProperties.isNotIntegrationTest) {
                throw RuntimeException("data not deleted. This is not running in an integration environment")
            }
            intraDayStockRecordRepository.deleteAll()
        }

        /*** Test code --------------------------------------------------**/
        fun validateEntry(
            date: Date,
            position: Int,
            referenceEntry: IntraDayStockRecord
        )  {
            val recordToValidate = intraDayStockRecordRepository.findAllByCreationDate(creationDate = date)[position]
            DomainValueCompareUtil.equalInValue(
                    object1 = referenceEntry,
                    object2 = recordToValidate
            )
        }

        fun entriesExistForDate(date: Date) : Boolean {
            return intraDayStockRecordRepository.findAllByCreationDate(creationDate = date).isNotEmpty()
        }
    }
}