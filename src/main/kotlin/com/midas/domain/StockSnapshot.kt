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
import java.util.concurrent.*
import kotlin.math.abs

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
        @Autowired val applicationProperties      : ApplicationProperties,
        @Autowired val stockSnapshotRepository    : StockSnapshotRepository,
        @Autowired val loggingService             : LoggingService,
        @Autowired private val tickerSpringAdapter: Ticker.SpringAdapter
    ) {
        @PostConstruct
        fun init() {
            StockSnapshot.applicationProperties   = applicationProperties
            StockSnapshot.stockSnapshotRepository = stockSnapshotRepository
            StockSnapshot.loggingService          = loggingService
            tickerSpringAdapter.init()
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
        val WINDOWS: List<Int> = listOf(5, 10, 20, 40, 60)
        private val activeTickers: MutableSet<String> = HashSet()

        fun findByDescending(ticker: String) : List<StockSnapshot> {
            if(snapshotMap.isEmpty()) {
                buildTickerSnapshotMap()
            }
            return snapshotMap[ticker] ?: emptyList()
        }
        private fun buildTickerSnapshotMap() {
            println("Mapping snapshots to tickers.....")
            val results: List<StockSnapshot> = stockSnapshotRepository.findAll().toList()
            for(r in results) {
                snapshotMap.computeIfAbsent(r.ticker.uppercase()) { mutableListOf()}.add(r)
                activeTickers.add(r.ticker.uppercase())
            }
            for(k in snapshotMap.keys) {
                snapshotMap[k] = snapshotMap[k]!!.sortedByDescending { it.creationDate }.toMutableList()
            }
            println("Tickers loaded....")
        }

        fun populatePastOneYearSnapshots() {
            populateSnapshots(days = 365)
        }

        fun populatePastOneMonthSnapshots() {
            populateSnapshots(days = 30)
        }

        private fun populateSnapshots(days: Int) {
            var tempDate = getCurrentDateString()
            loggingService.log("Initial date: $tempDate")

            for(i in 0 until days) {
                importSnapShots(tempDate)
                tempDate = decrementDateString(input = tempDate)
            }
        }

        fun delta(x2: StockSnapshot, x1: StockSnapshot) : Double {
            return ((x2.price - x1.price)/ x1.price)*100.0
        }

        fun max(currentPrice: Double, s: StockSnapshot) : Double {
            if(currentPrice> s.price) {
                return currentPrice
            }
            return s.price
        }

        fun min(currentPrice: Double, s: StockSnapshot) : Double {
            if(currentPrice < s.price) {
                return currentPrice
            }
            return s.price
        }

        fun calculateStatistics() {
            buildTickerSnapshotMap() //TODO: This populates the tickersnapshot map but also activeTickers. Needs to be broken up
            var c                           = 0
            var newTickers                  = 0
            var staleTickers                = 0
            val tickers: MutableSet<String> = HashSet(Ticker.getTickers())
            val oldNumTickers               = tickers.size
            val activeTickersSize           = activeTickers.size
            loggingService.log("Adding new tickers...")
            for (t0 in activeTickers) {
                if (!tickers.contains(t0)) {
                    Ticker.save(t0)
                    tickers.add(t0)
                    newTickers++
                }
            }
            loggingService.log("New tickers added: $newTickers")

            loggingService.log("Removing stale tickers...")
            /*Remove stale tickers **/
            for(t1 in tickers) {
                if(!activeTickers.contains(t1)) {
                    Ticker.delete(t1)
                    staleTickers++
                }
            }
            loggingService.log("Stale tickers removed: $staleTickers")

            for (t: String in tickers) { loggingService.log("Ticker: $c")
                c++
                val snapshots: List<StockSnapshot> = findByDescending(ticker = t)
                for (w in WINDOWS) {
                    var maxDelta         = 0.0
                    var maxPrice         = 0.0
                    var minPrice         = Double.MAX_VALUE
                    var minDelta         = Double.MAX_VALUE
                    var averageVolume    = 0.0
                    var averageDelta     = 0.0
                    var averageDeviation = 0.0

                    for (i in 1 until w) {
                        if (i >= snapshots.size) {break}
                        val currentDelta = delta(x2 = snapshots[i - 1], x1 = snapshots[i])
                        averageDelta += currentDelta
                        if (currentDelta > maxDelta) {
                            maxDelta = currentDelta
                        }
                        if (currentDelta < minDelta) {
                            minDelta = currentDelta
                        }

                        maxPrice = max(currentPrice = maxPrice, s = snapshots[i - 1])
                        minPrice = min(currentPrice = minPrice, s = snapshots[i - 1])
                        averageVolume += snapshots[i - 1].volume
                    }
                    averageVolume /= w
                    averageDelta  /= w

                    for (i in 1 until w) {
                        if (i >= snapshots.size) {break}
                        val currentDelta = delta(x2 = snapshots[i - 1], x1 = snapshots[i])
                        //if(averageDelta != 0.0) {
                            averageDeviation += abs(averageDelta - currentDelta)//(100 * abs(averageDelta - currentDelta)) / averageDelta
                        //}
                    }
                    averageDeviation /= w

                    var windowDelta = 0.0
                    if(snapshots.size >= w) {
                        windowDelta = delta(x2 = snapshots[0], x1 = snapshots[w - 1])
                    }
                    Statistics.save(
                        Statistics(
                            ticker           = t,
                            minPrice         = minPrice,
                            maxPrice         = maxPrice,
                            maxDelta         = maxDelta,
                            minDelta         = minDelta,
                            averageDelta     = averageDelta,
                            averageDeviation = averageDeviation,
                            averageVolume    = averageVolume,
                            windowDelta      = windowDelta,
                            timeWindow       = w,
                            count            = snapshots.size
                        )
                    )
                }
            }
            loggingService.log("New tickers: $newTickers")
            loggingService.log("Old number of tickers: $oldNumTickers")
            loggingService.log("Active tickers: $activeTickersSize")
            loggingService.log("Stale tickers removed: $staleTickers")
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
            if(queryCount == 0L) {
                //importSnapShotsWorker(dateString = decrementDateString(input = dateString))
                return
            }

            val creationDate: Date = SimpleDateFormat("yyyy-MM-dd").parse(dateString)
            val results = result["results"] as JSONArray
            val snapShots: List<StockSnapshot> = results.toList().filter {
                ((it as JSONObject)["T"] != null)&&
                (it["c"] != null) && (it["v"] != null) && (Etl.double(it["c"]) > 0.10)

            }.map {
                StockSnapshot(
                    ticker       = ((it as JSONObject)["T"] as String).uppercase(),
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