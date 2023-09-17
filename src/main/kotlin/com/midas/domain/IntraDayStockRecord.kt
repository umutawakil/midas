package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.interfaces.IntraDayMarketWebService
import com.midas.interfaces.ExecutionWindowPicker
import com.midas.repositories.IntraDayStockRecordRepository
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
    private val hour: Int
    @Column
    private val minute: Int
    @Column
    private val externalTime: Long
    @Temporal(TemporalType.DATE)
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
            hour: Int,
            minute: Int,
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
        this.hour                   = hour
        this.minute                 = minute
        this.externalTime           = externalTime
        this.creationDate           = creationDate
    }

    @Component
    private class SpringAdapter(
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val intraDayStockRecordRepository: IntraDayStockRecordRepository
    ) {
        @PostConstruct
        fun init() {
            IntraDayStockRecord.applicationProperties         = applicationProperties
            IntraDayStockRecord.intraDayStockRecordRepository = intraDayStockRecordRepository
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var intraDayStockRecordRepository: IntraDayStockRecordRepository

        fun downloadContinuously(
            date                    : Date,
            executionWindowPicker   : ExecutionWindowPicker,
            intraDayMarketWebService: IntraDayMarketWebService
        ) {
            /*TODO: Needs to sleep but in such a way that its not spinning the CPU to death but not so slow
            *   it runs the risk of the executionWindow being missed because its sleeping over it.
            *
            *   Perhaps it checks every 30 seconds and has a overlap allowance of 5 minutes
            * */
            try {
                download(
                    date,
                    executionWindowPicker,
                    intraDayMarketWebService
                )
            } catch(ex: Exception) {
                //TODO: Need to notify me the run failed
                println("Download failed. Sending push notification................")
                ex.printStackTrace()
            }
        }

        /**TODO: TO test run with ew[9:30], ew[10:00], ew[10:30]
         *  ew[12:30](This should produce a stock record but no diff since its previous doesn't exist but ew[13] should succeed,
         *  Time[14:17](should produce no record or delta "No execution") Just run it at any non-execution time
          */
        private fun download(
            date                    : Date,
            executionWindowPicker   : ExecutionWindowPicker,
            intraDayMarketWebService: IntraDayMarketWebService
        ) {
            val unValidatedWindow = executionWindowPicker.getExecutionWindow()
            val executionWindow   = validateWindow(hour = unValidatedWindow[0],minutes = unValidatedWindow[1]) ?: return

            if (!intraDayMarketWebService.isMarketOpen()) {
                println("Download skipped. Market is not open.")
                return
            }

            ingest(
                    todaysDate      = date,
                    executionWindow = executionWindow,
                    stockRecords    = intraDayMarketWebService.downloadRecords()["tickers"] as JSONArray
            )
        }
        private fun ingest(
                todaysDate: Date,
                executionWindow: Array<Int>,
                stockRecords: JSONArray
        ) {
            val recordsByTicker: MutableMap<String, MutableList<IntraDayStockRecord>> = loadRecordsByTicker(date = todaysDate)
            var errorFileWritten = false
            println("Ingesting...")
            for(i in 0 until stockRecords.size) {
                val r = stockRecords[i] as JSONObject
                try {
                    val previousDayClose  = Etl.double((r["prevDay"] as JSONObject)["c"])
                    val todaysChange      = Etl.double((r["todaysChange"]))
                    val ticker            = r["ticker"] as String
                    val price             = Etl.double(previousDayClose + todaysChange)
                    val previousDayVwap   = Etl.double((r["prevDay"] as JSONObject)["vw"])

                    Ticker.saveIfNotExist(symbol = ticker)

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
                            hour                   = executionWindow[0],
                            minute                 = executionWindow[1],
                            externalTime           = (r["updated"] as Long),
                            creationDate           = todaysDate
                        )
                    )

                    if((executionWindow[0] == 9) && (executionWindow[1] == 30)) { /* 1st record of the day*/
                        buildDeltaFromStockRecord(
                            newRecord           = newRecord,
                            priceChangePercent  = ((newRecord.openPrice - previousDayClose) / previousDayClose) * 100,
                            vwapChangePercent   = ((newRecord.vwap      - previousDayVwap)  / previousDayVwap)  * 100,
                            volumeChangePercent = 0.0
                        )
                    } else {
                        /** If for any reason there's no previous stock record
                         * then no delta record will be calculated until the next cycle. **/
                        val previousRecord = getPreviousRecordInExecutionWindow(
                            ticker          = ticker,
                            hour            = executionWindow[0],
                            minute          = executionWindow[1],
                            recordsByTicker = recordsByTicker
                        ) ?: continue

                        buildDeltaFromStockRecord(
                            newRecord           = newRecord,
                            priceChangePercent  = ((newRecord.price             - previousRecord.price)/previousRecord.price) * 100,
                            volumeChangePercent = ((newRecord.accumulatedVolume - previousRecord.accumulatedVolume)/previousRecord.accumulatedVolume) * 100.0,
                            vwapChangePercent   = ((newRecord.vwap              - previousRecord.vwap)/previousRecord.vwap) * 100
                        )
                    }

                } catch (ex: Exception) {
                    println("Error processing stock record. See file for details -> ")
                    println("")
                    ex.printStackTrace()

                    if(!errorFileWritten) {
                        writeDataToDiskForInspection(stockRecords = stockRecords)
                        errorFileWritten = true
                    }
                }
            }
        }

        private fun buildDeltaFromStockRecord(
                newRecord           :IntraDayStockRecord,
                priceChangePercent  :Double,
                volumeChangePercent :Double,
                vwapChangePercent   :Double,
        ) {
            val volatilityEstimate        = ((newRecord.previousDayHigh - newRecord.previousDayLow) / newRecord.previousDayOpen) * 100
            val runningVolatilityEstimate = (newRecord.highPrice - newRecord.lowPrice)/newRecord.openPrice * 100

            DeltasOfStockIndicators.save(
                    DeltasOfStockIndicators(
                        ticker                          = newRecord.ticker,
                        priceChangePercent              = priceChangePercent,
                        volumeChangePercent             = volumeChangePercent,
                        vwapChangePercent               = vwapChangePercent,
                        volumePriceDeltaRatio           = if(priceChangePercent == 0.0) { Double.MAX_VALUE} else {((volumeChangePercent /priceChangePercent)*100)},
                        volatilityEstimate              = volatilityEstimate,
                        priceDeltaVolatilityRatio       = (priceChangePercent / volatilityEstimate) * 100,
                        priceDeltaVolatilityDiff        = priceChangePercent - volatilityEstimate,
                        runningVolatilityEstimate       = runningVolatilityEstimate,
                        todayPreviousVolatilityDelta    = (runningVolatilityEstimate/volatilityEstimate) * 100,
                        hour                            =  newRecord.hour,
                        minute                          =  newRecord.minute,
                        externalTime                    =  newRecord.externalTime,
                        creationDate                    =  newRecord.creationDate
                    )
            )
        }

        private fun loadRecordsByTicker(date: Date) : MutableMap<String, MutableList<IntraDayStockRecord>> {
            val map: MutableMap<String, MutableList<IntraDayStockRecord>> = HashMap()
            for(r in intraDayStockRecordRepository.findAllByCreationDate(creationDate = date)) {
                map.computeIfAbsent(r.ticker) { mutableListOf() }.add(r)
            }
            return map
        }

        @Component
        private class PolyGonService : IntraDayMarketWebService {
            override fun downloadRecords() : JSONObject {
                val url: String = applicationProperties.polygonAllTickersURL +
                        "?apiKey=${applicationProperties.polyGonApiKey}&include_otc=true"
                return HttpUtility.getJSONObject(inputURL = url)
            }

            override fun isMarketOpen(): Boolean {
                val calendar = Calendar.getInstance()
                calendar.timeZone = Calendar.getInstance().timeZone//TimeZone.getTimeZone("America/New_York")
                val dayOfTheWeek: Int = calendar[Calendar.DAY_OF_WEEK]
                println("dayOfTheWeek: $dayOfTheWeek")
                if (dayOfTheWeek == 1 || dayOfTheWeek == 7) {
                    println("skipping for the weekend")
                    return false
                }

                val url:String = applicationProperties.polygonMarketStatusURL+"?apiKey=${applicationProperties.polyGonApiKey}"
                val r: JSONObject = HttpUtility.getJSONObject(inputURL = url)

                val nycStatus =  (r["exchanges"] as JSONObject)["nyse"] as String // closed, open, extended-hours
                println("Market values -> NYC: $nycStatus")
                return nycStatus == "open"
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

        private fun validateWindow(hour: Int, minutes: Int): Array<Int>? {
            /* 4:00 to 16:00 */
            val times = listOf(
                arrayOf(9, 30),
                arrayOf(10, 30),
                arrayOf(11, 30),
                arrayOf(12, 30),
                arrayOf(13, 30),
                arrayOf(14, 30),
                arrayOf(15, 3),
                arrayOf(16, 0)
            )
            for (i in times.indices) {
                val h = times[i][0]
                val m = times[i][1]
                if (hour != h) {
                    continue
                }
                //println("Hour matches: $hour, m: $m")
                if ((minutes - m > 1) || (minutes - m < 0)) {
                    continue
                }
                //println("Execution window: $h: $m")
                return arrayOf(h, m)
            }
            return null
        }

        @Component
        private class MidasExecutionWindowPicker : ExecutionWindowPicker {
            override fun getExecutionWindow(): Array<Int> {
                val calendar = Calendar.getInstance()
                calendar.timeZone = Calendar.getInstance().timeZone//TimeZone.getTimeZone("America/New_York")
                return arrayOf(calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE])
            }
        }

        //TODO: This seems to be a place where boundary conditions could lead to bugs.
        private fun getPreviousRecordInExecutionWindow(
                ticker: String,
                hour  : Int,
                minute: Int,
                recordsByTicker: MutableMap<String, MutableList<IntraDayStockRecord>>

        ) : IntraDayStockRecord? {
            val recordsForToday: MutableList<IntraDayStockRecord> = recordsByTicker[ticker] ?: return null
            val lastRecord = recordsForToday[recordsForToday.size - 1]

            if (
                 ((lastRecord.hour == hour - 1) && (lastRecord.minute == minute)) ||
                 ((hour == 16) && (minute == 0))
               )
            {
                return lastRecord
            }
            return null
        }

        private fun save(record: IntraDayStockRecord) : IntraDayStockRecord {
            return intraDayStockRecordRepository.save(record)
        }

        fun deleteAll() {
            if(!applicationProperties.isIntegrationTest) {
                throw RuntimeException("data not deleted. This is not running in an integration environment")
            }
            intraDayStockRecordRepository.deleteAll()
            DeltasOfStockIndicators.deleteAll()
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