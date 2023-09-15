package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.interfaces.IntraDayMarketWebService
import com.midas.interfaces.ExecutionWindowPicker
import com.midas.repositories.IntraDayStockRecordRepository
import com.midas.utilities.DomainValueCompareUtil
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
    private val accumulatedVolume: Long

    @Column
    private val todaysChange: Double
    @Column
    private val todaysChangePercentage: Double

    @Column
    private val previousDayVolume: Long
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
            accumulatedVolume: Long,
            todaysChange: Double,
            todaysChangePercentage: Double,
            previousDayVolume: Long,
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
            val executionWindow   = unValidatedWindow//validateWindow(hour = unValidatedWindow[0],minutes = unValidatedWindow[1]) ?: return

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
            for(i in 0 until stockRecords.size) {
                println("Record:$i")
                val r = stockRecords[i] as JSONObject
                try {
                    val previousDayClose  = (r["prevDay"] as JSONObject)["c"] as Double
                    val todaysChange      = (r["todaysChange"] as Double)
                    val ticker            = r["ticker"] as String
                    val price             = previousDayClose + todaysChange
                    val previousDayVwap   = (r["prevDay"] as JSONObject)["vw"] as Double

                    Ticker.saveIfNotExist(symbol = ticker)

                    val newRecord = save(
                        IntraDayStockRecord(
                            ticker                 = ticker,
                            price                  = price,
                            vwap                   = (r["min"] as JSONObject)["vw"] as Double,
                            openPrice              = (r["day"] as JSONObject)["o"] as Double,
                            accumulatedVolume      = (r["min"] as JSONObject)["av"] as Long,
                            todaysChange           = todaysChange,
                            todaysChangePercentage = (r["todaysChangePerc"] as Double),
                            previousDayVolume      = (r["prevDay"] as JSONObject)["v"] as Long,
                            previousDayClose       = previousDayClose,
                            previousDayOpen        = (r["prevDay"] as JSONObject)["o"] as Double,
                            previousDayHigh        = (r["prevDay"] as JSONObject)["h"] as Double,
                            previousDayLow         = (r["prevDay"] as JSONObject)["l"] as Double,
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
                        /** It's important to note this is for all time windows that have a previous. So if a
                         * disaster occurs and theres no previous record then no deltas are built for that time window. **/
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
            DeltasOfStockIndicators.save(
                    DeltasOfStockIndicators(
                            ticker              = newRecord.ticker,
                            priceChangePercent  = priceChangePercent,
                            volumeChangePercent = volumeChangePercent,
                            vwapChangePercent   = vwapChangePercent,
                            volatilityEstimate  = ((newRecord.previousDayHigh - newRecord.previousDayLow) / newRecord.previousDayOpen) * 100,
                            hour                =  newRecord.hour,
                            minute              =  newRecord.minute,
                            externalTime        =  newRecord.externalTime,
                            creationDate        =  newRecord.creationDate
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
                        "?apiKey=${applicationProperties.polyGonApiKey}"
                return HttpUtility.getJSONObject(inputURL = url)

                /*val status = r["status"] as String?
                if ((status == null) || (status != "OK")) {
                    println("Status: $status")
                    println("Status was not OK")
                    throw RuntimeException(r.toJSONString())
                }
                return r*/
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
                return nycStatus != "closed"
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

        fun validateWindow(hour: Int, minutes: Int): Array<Int>? {
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
            val newRecord  = intraDayStockRecordRepository.save(record)
            return newRecord
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