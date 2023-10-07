package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.StockSnapshotRepository
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Entity
@Table(name="stock_snapshot")
class StockSnapshot {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id           : Long = -1L
    private val ticker       : String
    private val price        : Double
    private var volume       : Int
    private val creationDate : Date

    constructor(ticker: String, price: Double, volume: Int, creationDate: Date) {
        this.ticker       = ticker
        this.price        = price
        this.volume       = volume
        this.creationDate = creationDate
    }

    @Component
    class SpringAdapter(
        @Autowired val applicationProperties  : ApplicationProperties,
        @Autowired val stockSnapshotRepository: StockSnapshotRepository,
        @Autowired val loggingService         : LoggingService
    ) {
        @PostConstruct
        fun init() {
            StockSnapshot.applicationProperties   = applicationProperties
            StockSnapshot.stockSnapshotRepository = stockSnapshotRepository
            StockSnapshot.loggingService          = loggingService
        }
    }

    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var stockSnapshotRepository: StockSnapshotRepository
        private lateinit var loggingService: LoggingService
        private val          executorService: ExecutorService = ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue()
        )
        private val snapshotMap: MutableMap<String,MutableList<StockSnapshot>> = HashMap()

        fun findByDescending(ticker: String) : List<StockSnapshot> {
            if(snapshotMap.isEmpty()) {
                loadMap()
            }
            return snapshotMap[ticker] ?: emptyList()
        }
        private fun loadMap() {
            println("Loading tickers.....")
            val results: List<StockSnapshot> = stockSnapshotRepository.findAll().toList()
            for(r in results) {
                snapshotMap.computeIfAbsent(r.ticker) { mutableListOf()}.add(r)
            }
            for(k in snapshotMap.keys) {
                snapshotMap[k]!!.sortedByDescending { it.creationDate }
            }
            println("Tickers loaded....")
        }

        /**
         * work
         * min date 2022-10-06
         * max date 2023-10-04
         */

        fun populatePastOneYearSnapshots() {
            var tempDate = getCurrentDateString()
            loggingService.log("Initial date: $tempDate")
            for(i in 0 until 365) {
                importSnapShots(tempDate)
                tempDate = decrementDateString(input = tempDate)
            }
        }
        fun delta(x2: StockSnapshot, x1: StockSnapshot) : Double {
            return ((x2.price - x1.price)/ x1.price)*100.0
        }

        private fun importSnapShots(dateString: String) {
            executorService.execute {
                try {
                    loggingService.log("importing: $dateString")
                    importSnapShotsWorker(dateString = dateString)
                } catch (e: Exception) {
                    loggingService.error(e)
                }
            }
        }
        private fun importSnapShotsWorker(dateString: String) {
            val url: String = "${applicationProperties.polygonBaseUrl}/v2/aggs/grouped/locale/us/market/stocks/$dateString"+
                    "?apiKey=${applicationProperties.polyGonApiKey}&include_otc=true"

            val result: JSONObject = HttpUtility.getJSONObject(inputURL = url)
            val queryCount: Long = result["queryCount"] as Long
            if(queryCount == 0L) return

            val creationDate: Date = SimpleDateFormat("yyyy-MM-dd").parse(dateString)
            val results = result["results"] as JSONArray
            val snapShots: List<StockSnapshot> = results.toList().filter {
                ((it as JSONObject)["T"] != null)&&
                (it["c"] != null) && (it["v"] != null) && (Etl.double(it["c"]) > 1.00)

            }.map {
                StockSnapshot(
                    ticker       = (it as JSONObject)["T"] as String,
                    price        = Etl.double(it["c"]),
                    volume       = Etl.integer(it["v"]),
                    creationDate = creationDate
                )
            }
            snapShots.forEach {
                stockSnapshotRepository.save(it)
            }
        }

        private fun getCurrentDateString() : String {
            return SimpleDateFormat("yyyy-MM-dd").
            format(Date(System.currentTimeMillis()))
        }

        private fun decrementDateString(input: String) : String {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
            val calendar      = Calendar.getInstance()
            calendar.time     = dateFormatter.parse(input)

            calendar.add(Calendar.DATE, -1)
            return dateFormatter.format(calendar.time)
        }
    }
}