package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.TickerRepository
import com.midas.services.LoggingService
import com.midas.utilities.HttpUtility
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

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

            var results: List<Ticker> = tickerRepository.findAll().toList()
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

        fun getTickers() : List<String> {
            return this.tickerCache.values.map { it.name}
        }

        /**
         * INSERT INTO Customers (CustomerName, City, Country)
         * SELECT SupplierName, City, Country FROM Suppliers;
         */
        fun importTickers() {
            val url: String = applicationProperties.polygonBaseUrl +
                    "/v3/reference/tickers?apiKey=${applicationProperties.polyGonApiKey}&include_otc=true"

            val result: JSONObject = HttpUtility.getJSONObject(inputURL = url)
            val tickerObjects = result["results"] as JSONArray

            val tickers: List<Ticker> = tickerObjects.toList().filter{
                ((it as JSONObject)["locale"] as String) == "us"
            }.map {
                Ticker(name = (it as JSONObject)["ticker"] as String)
            }
            tickers.forEach {
                tickerRepository.save(it)
            }
        }
    }

}