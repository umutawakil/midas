package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.TickerRepository
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

/** TODO: THis needs to update itself regularly or have the stock snapshot logic update ticker by ticker**/
@Entity
@Table(name="ticker")
class Ticker {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id  : Long = -1L
    private val name: String
    constructor(name: String) {
        this.name = name
    }

    @Component
    class SpringAdapter(
        @Autowired private val tickerRepository: TickerRepository,
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val loggingService: LoggingService
    ) {
        @PostConstruct
        fun init() {
            if(tickerCache.isNotEmpty()) {
                return
            }
            Ticker.applicationProperties = applicationProperties
            Ticker.tickerRepository      = tickerRepository
            Ticker.loggingService        = loggingService

            var results: List<Ticker> = tickerRepository.findAll().toList()
            if(results.isEmpty()) {
                importTickers()
                results = tickerRepository.findAll().toList()
            }
            loggingService.log("Tickers loaded from DB:  (${results.size}) loaded")
            results.forEach {
                tickerCache[it.name] = it
            }
        }

    }
    companion object {
        private lateinit var tickerRepository: TickerRepository
        private val tickerCache: MutableMap<String, Ticker> = mutableMapOf()
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var loggingService: LoggingService
        fun getTickers() : Set<String> {
            return this.tickerCache.values.map { it.name}.toSet()
        }

        private fun importTickers() {
            loggingService.log("Importing tickers from service because DB table is empty...")
            val url: String = applicationProperties.polygonAllTickersURL +
                    "?apiKey=${applicationProperties.polyGonApiKey}&include_otc=true"
            val results: JSONArray = HttpUtility.getJSONObject(inputURL = url)["tickers"] as JSONArray
            val tickers: MutableSet<String> = mutableSetOf()
            for (i in results.indices) {
                tickers.add(((results[i] as JSONObject)["ticker"] as String).uppercase())
            }
            for(t in tickers) {
                tickerRepository.save(Ticker(name = t))
            }
            loggingService.log("Ticker import complete")
        }
        fun save(t: String) {
           // tickerCache[t] = //TODO: This should happen but at the moment that will cause problems because we traverse the  set while updating it.
                tickerRepository.save(Ticker(name = t))
        }

        fun delete(t: String) {
            tickerRepository.delete(tickerCache[t]!!)
        }
    }

}