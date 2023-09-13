package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.IntraDayStockRecordRepository
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
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Usman Mutawakil on 2020-04-02.
 */
@Component
@Entity
@Table(name = "intra_day_stock_record")
class IntraDayStockRecord {
    @Component
    private class SpringAdapter(
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val intraDayStockRecordRepository: IntraDayStockRecordRepository
    ) {
        @PostConstruct
        fun init() {
            IntraDayStockRecord.applicationProperties         = applicationProperties
            IntraDayStockRecord.intraDayStockRecordRepository = intraDayStockRecordRepository

            for(r in intraDayStockRecordRepository.findAllByDate(date = Date(System.currentTimeMillis()))) {
                recordsByTicker.computeIfAbsent(r.ticker) { mutableListOf() }.add(r)
            }
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var intraDayStockRecordRepository: IntraDayStockRecordRepository
        private val          recordsByTicker: MutableMap<String, MutableList<IntraDayStockRecord>> = ConcurrentHashMap()

        fun downloadContinuously() {
            //TODO: Needs to sleep
            download()
        }

        /**TODO: TO test run with ew[9:30], ew[10:00], ew[10:30]
         *  ew[12:30](This should produce a stock record but no diff since its previous doesn't exist but ew[13] should succeed,
         *  Time[14:17](should produce no record or delta "No execution") Just run it at any non-execution time
         *
         *  Do we need to save initial json responses to files in order to verify the data in the db makes sense?
          */
        private fun download() {
            val formatter       = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val currentDateTime = LocalDateTime.now().format(formatter)
            val executionWindow = getExecutionWindow()
            if(executionWindow == null) {
                println("No execution")
                return
            }

            if(!isMarketOpen()) {
                println("Download skipped. Market is not open.")
            }

            //val file = File("/Users/umutawakil/Documents/Git/midas/src/main/resources/intra-day.json")
            //val response: JSONObject = JSONParser().parse(file.readText()) as JSONObject
            val stockRecords = downloadIntraDayStockRecordsFromExternal()

            //TODO: This should be paramerized to a configurable location. Will function as a dead letter queue. Can even restructure the code so this save takes place if any exception occurs.
            val file = File("/Users/umutawakil/Documents/Git/midas/src/main/resources/intra-day-$currentDateTime.json")
            file.printWriter().print(stockRecords.toJSONString())

            for(i in 0 until stockRecords.size) {
                val r = stockRecords[i] as JSONObject
                try {
                    val previousDayClose  = (r["prevDay"] as JSONObject)["c"] as Double
                    val todaysChange      = (r["todaysChange"] as Double)
                    val ticker            = r["ticker"] as String
                    val price             = previousDayClose + todaysChange
                    val previousDayVwap   = (r["prevDay"] as JSONObject)["vw"] as Long
                    val previousDayVolume = (r["prevDay"] as JSONObject)["v"] as Long

                    Ticker.saveIfNotExist(symbol = ticker)

                    val newRecord = save(
                        IntraDayStockRecord(
                            ticker                 = ticker,
                            price                  = price,
                            vwap                   = (r["min"] as JSONObject)["vw"] as Double,
                            openPrice              = (r["day"] as JSONObject)["o"] as Double,
                            accumulatedVolume      = (r["min"] as JSONObject)["av"] as Long,
                            previousDayClose       = previousDayClose,
                            todaysChange           = todaysChange,
                            todaysChangePercentage = (r["todaysChangePerc"] as Double),
                            previousDayVolume      = (r["prevDay"] as JSONObject)["v"] as Long,
                            hour                   = executionWindow[0],
                            minute                 = executionWindow[1],
                            externalTime           = (r["updated"] as Long)
                        )
                    )


                    //TODO: How do we retrieve the previous hour to hour record for the current day?
                    if((executionWindow[0] == 9) && (executionWindow[1] == 30)) { //1st record of the day
                        StockIndicatorsDelta.save(
                            StockIndicatorsDelta(
                                ticker              = ticker,
                                priceChangePercent  = ((newRecord.openPrice         - previousDayClose) /previousDayClose)  * 100,
                                volumeChangePercent = 0.0, //TODO: To calculate this correctly we need previousDay.accumulatedVolume*previousDelta.volumeChangePercent (vwap should be sufficient for now)
                                vwapChangePercent   = ((newRecord.vwap              - previousDayVwap)  /previousDayVwap)   * 100,
                                hour                =  newRecord.hour,
                                minute              =  newRecord.minute,
                                externalTime        =  newRecord.externalTime,
                                creationDate        =  newRecord.creationDate
                            )
                        )
                    } else {
                        val previousRecord = getPreviousRecordInExecutionWindow(
                            ticker = ticker,
                            hour   = executionWindow[0],
                            minute = executionWindow[1]
                        ) ?: continue

                        StockIndicatorsDelta.save(
                                StockIndicatorsDelta(
                                ticker              = ticker,
                                priceChangePercent  = ((newRecord.price             - previousRecord.price)/previousRecord.price) * 100,
                                volumeChangePercent = ((newRecord.accumulatedVolume - previousRecord.accumulatedVolume)/previousRecord.accumulatedVolume) * 100.0,
                                vwapChangePercent   = ((newRecord.vwap              - previousRecord.vwap)/previousRecord.vwap) * 100,
                                hour                =  newRecord.hour,
                                minute              =  newRecord.minute,
                                externalTime        =  newRecord.externalTime,
                                creationDate        =  newRecord.creationDate
                            )
                        )
                    }

                } catch (ex: Exception) {
                    println("Error processing stock record")
                    println("Record: " + r.toJSONString())
                    println("")
                    ex.printStackTrace()
                }
            }
        }

        private fun downloadIntraDayStockRecordsFromExternal() : JSONArray {
            val url: String = applicationProperties.polygonAllTickersURL +
                    "?apiKey=${applicationProperties.polyGonApiKey}"
            val r: JSONObject = HttpUtility.getJSONObject(inputURL = url)

            val file = File("/Users/umutawakil/Documents/Git/midas/src/main/resources/intra-day-${System.currentTimeMillis()}.json")
            file.printWriter().print(r.toJSONString())

            val status = r["status"] as String?
            if ((status == null) || (status != "OK")) {
                println("Status: $status")
                println("Status was not OK")
                throw RuntimeException(r.toJSONString())
            }
            if (r == null || r.isEmpty()) {
                throw RuntimeException("No data returned for intra-day stock records: $url")
            }
            return r["tickers"] as JSONArray
        }

        private fun getExecutionWindow() : Array<Int>? {
            /* 4:00 to 16:00 */
            val times = listOf(
                /*arrayOf(4,0),
                arrayOf(4,30),
                arrayOf(5,0),
                arrayOf(5,30),
                arrayOf(6,0),
                arrayOf(6,30),
                arrayOf(7,0),
                arrayOf(7,30),
                arrayOf(8,0),
                arrayOf(8,30),
                arrayOf(9,0),*/
                arrayOf(9,30),
                arrayOf(10,0),
                arrayOf(10,30),
                arrayOf(11,0),
                arrayOf(11,30),
                arrayOf(12,0),
                arrayOf(12,30),
                arrayOf(13,0),
                arrayOf(13,30),
                arrayOf(14,0),
                arrayOf(14,30),
                arrayOf(15,0),
                arrayOf(15,3),
                arrayOf(16,0)
            )
            val calendar      = Calendar.getInstance()
            calendar.timeZone = TimeZone.getTimeZone("America/New_York")
            val hour          = calendar[Calendar.HOUR_OF_DAY]
            val minutes       = calendar[Calendar.MINUTE]

            for(i in times.indices) {
                val h = times[i][0]
                val m = times[i][1]
                if(hour != h) {
                    continue
                }
                println("Hour matches: $hour, m: $m")
                if((minutes - m > 1) || (minutes - m < 0)) {
                    continue
                }
                println("Execution window: $h: $m")
                return arrayOf(h,m)
            }
            return null
        }

        private fun isMarketOpen(): Boolean {
            val url:String = applicationProperties.polygonMarketStatusURL+"?apiKey=${applicationProperties.polyGonApiKey}"
            println("URL: $url")

            val r: JSONObject = HttpUtility.getJSONObject(inputURL = url)
            if(r == null || r.isEmpty()) {
                throw RuntimeException("No data returned for market status check: $url")
            }
            val nycStatus =  (r["exchanges"] as JSONObject)["nyse"] as String // closed, open, extended-hours
            /*val afterHours = r["afterHours"] as Boolean
            val earlyHours = r["afterHours"] as Boolean*/
            val market     = r["market"]     as String

            println("Market values -> NYC: $nycStatus")
           // println("Market values -> NYC: $nycStatus, earlyHours: $earlyHours, afterHours: $afterHours")

            return nycStatus != "closed"
        }

        private fun updateCache(record: IntraDayStockRecord) {
            recordsByTicker.computeIfAbsent(record.ticker) { mutableListOf() }.add(record)
        }

        //TODO: This seems to be a place where boundary conditions could lead to bugs.
        private fun getPreviousRecordInExecutionWindow(ticker: String, hour: Int, minute: Int) : IntraDayStockRecord? {
            val recordsForToday: List<IntraDayStockRecord> = recordsByTicker[ticker] ?: return null
            if (recordsForToday.isEmpty()) { return null }

            val lastRecord = recordsForToday[recordsForToday.size - 1]
            if (
                    ((lastRecord.hour == hour - 1) && (lastRecord.minute == 30) && (minute == 0)) ||
                    ((lastRecord.hour == hour)     && (lastRecord.minute == 0)  && (minute == 30))
                )
            {
                return lastRecord
            }
            return null
        }

        private fun save(record: IntraDayStockRecord) : IntraDayStockRecord {
            val newRecord  = intraDayStockRecordRepository.save(record)
            updateCache(newRecord)
            return newRecord
        }
    }

    //@Entity(name="Subscription")

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
    private val previousDayClose: Double
    @Column
    private val todaysChange: Double
    @Column
    private val todaysChangePercentage: Double

    @Column
    private val previousDayVolume: Long

    @Column
    private val hour: Int
    @Column
    private val minute: Int
    @Column
    private val externalTime: Long
    @Column
    private val creationDate: Date

    constructor(
            ticker: String,
            price: Double,
            vwap: Double,
            openPrice: Double,
            accumulatedVolume: Long,
            previousDayClose: Double,
            todaysChange: Double,
            todaysChangePercentage: Double,
            previousDayVolume: Long,
            hour: Int,
            minute: Int,
            externalTime: Long
    ) {
        this.ticker                 = ticker
        this.price                  = price
        this.vwap                   = vwap
        this.openPrice              = openPrice
        this.accumulatedVolume      = accumulatedVolume
        this.previousDayClose       = previousDayClose
        this.todaysChange           = todaysChange
        this.todaysChangePercentage = todaysChangePercentage
        this.previousDayVolume      = previousDayVolume
        this.hour                   = hour
        this.minute                 = minute
        this.externalTime           = externalTime
        this.creationDate           = Date(System.currentTimeMillis())
    }
}