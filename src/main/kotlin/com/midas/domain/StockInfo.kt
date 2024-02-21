package com.midas.domain

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.midas.configuration.ApplicationProperties
import com.midas.repositories.StockInfoRepository
import com.midas.repositories.UnsupportedTickerRepository
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.Serializable


@Entity
@Table(name="v_stock_info")
class StockInfo : Serializable {
    @Embeddable
    class StockInfoId(val ticker: String, val timeWindow: Int) : Serializable

    @EmbeddedId
    val id: StockInfoId
    val name: String?
    val windowDelta: Double
    val minDelta: Double
    val maxDelta: Double
    val averageDeviation: Double
    val volumeDelta: Double
    val profitMargin: Double?
    @Column(name = "debt_percentage")
    val debtPercentage: Double?
    @Column(name = "cfo_working_capital")
    val cashBurnRate: Double?
    val secSectorCode: Int?
    val sicCode: Int?
    val otc: Boolean?

    constructor(
        ticker: String,
        name: String?,
        windowDelta: Double,
        minDelta: Double,
        maxDelta: Double,
        averageDeviation: Double,
        volumeDelta: Double,
        profitMargin: Double?,
        debtRatio: Double?,
        cashBurnRate: Double?,
        timeWindow: Int,
        secSectorCode: Int?,
        sicCode: Int?,
        otc: Boolean?
    ) {
        this.name             = name
        this.windowDelta      = windowDelta
        this.minDelta         = minDelta
        this.maxDelta         = maxDelta
        this.averageDeviation = averageDeviation
        this.volumeDelta      = volumeDelta
        this.profitMargin     = profitMargin
        this.debtPercentage   = debtRatio
        this.cashBurnRate     = cashBurnRate
        this.id               = StockInfoId(ticker = ticker, timeWindow = timeWindow)
        this.secSectorCode    = secSectorCode
        this.sicCode          = sicCode
        this.otc              = otc
    }

    @Component
    class SpringAdapter(
        @Autowired private val stockInfoRepository: StockInfoRepository,
        @Autowired private val unsupportedTickerRepository: UnsupportedTickerRepository,
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val loggingService: LoggingService
    ) {
        @PostConstruct
        fun init() {
            StockInfo.stockInfoRepository         = stockInfoRepository
            StockInfo.unsupportedTickerRepository = unsupportedTickerRepository
            StockInfo.applicationProperties       = applicationProperties
            StockInfo.loggingService              = loggingService
        }

    }
    companion object {
        private lateinit var stockInfoRepository         : StockInfoRepository
        private lateinit var unsupportedTickerRepository : UnsupportedTickerRepository
        private lateinit var applicationProperties       : ApplicationProperties
        private lateinit var loggingService              : LoggingService
        fun exportToCloud() {
            val results1 = stockInfoRepository.findAll().toList()
            loggingService.log("Loading stock info:  ${results1.size} records")

            val results2 = unsupportedTickerRepository.findAll().toList()
            loggingService.log("Loading unsupported tickers:  ${results2.size} records")

            val file1 = File("stock-info.json")
            if (file1.exists()) {
                file1.delete()
            }
            loggingService.log("New file: " + file1.createNewFile()+", file: " + file1.name)

            val file2 = File("unsupported-tickers.json")
            if (file2.exists()) {
                file2.delete()
            }
            loggingService.log("New file: " + file2.createNewFile() +", file:" + file2.name)

            writeDataToFile(file1, results1)
            writeDataToFile(file2, results2)

            val s3: AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build()
            s3.putObject(
                applicationProperties.deploymentBucket,
                file1.name,
                file1
            )
            s3.putObject(
                applicationProperties.deploymentBucket,
                file2.name,
                file2
            )
            loggingService.log("Data files uploaded")
        }

        private fun writeDataToFile(file: File, results: List<Any>) {
            val objectMapper = ObjectMapper()
            val out = ByteArrayOutputStream()
            objectMapper.writeValue(out, results)
            file.writeText(String(out.toByteArray()))
            loggingService.log("File written: " + file.absolutePath)
        }
    }
}