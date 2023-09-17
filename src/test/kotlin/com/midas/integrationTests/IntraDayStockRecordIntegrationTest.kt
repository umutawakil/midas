package com.midas.integrationTests

import com.midas.domain.DeltasOfStockIndicators
import com.midas.domain.IntraDayStockRecord
import com.midas.interfaces.IntraDayMarketWebService
import com.midas.interfaces.ExecutionWindowPicker
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest
class IntraDayStockRecordIntegrationTest(
        @Autowired private val intraDayStockRecord: IntraDayStockRecord
) {

    @BeforeEach
    fun setup() {
        println("Deleting intra day records...")
        IntraDayStockRecord.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        //IntraDayStockRecord.deleteAll()
    }

    @Test
    fun download_can_parse_opening_bell_and_first_hour() {
        val mockPolyGonWebService1              = MockPolyGonWebService()
        mockPolyGonWebService1.downloadResponse = getWebServiceData("/intra-day/download-1.json")
        mockPolyGonWebService1.marketOpen       = true
        val mockExecutionWindowPicker1          = MockExecutionWindowPicker()
        mockExecutionWindowPicker1.window       = arrayOf(9,30) //9:30 AM
        val date                                = Date(0)

        /** Test inputs that match what is in the test file **/
        val price                  = 20.506
        val vwap                   = 20.5105
        val openPrice              = 20.64
        val highPrice              = 20.64
        val lowPrice               = 20.506
        val accumulatedVolume      = 37216.0
        val todaysChange           = -0.124
        val todaysChangePercentage = -0.601
        val previousDayVolume      = 292738.0
        val previousDayClose       = 20.63
        val previousDayOpen        = 20.79
        val previousDayHigh        = 21.0
        val previousDayLow         = 20.5

        /** Download one file at "9:30" and validate results followed by one at 10: 30 **/
        IntraDayStockRecord.downloadContinuously(
            date                     = date,
            intraDayMarketWebService = mockPolyGonWebService1,
            executionWindowPicker    = mockExecutionWindowPicker1
        )

        IntraDayStockRecord.validateEntry(
                date            = date,
                position        = 0,
                    referenceEntry = IntraDayStockRecord(
                        ticker                 = "BCAT",
                        price                  = price,
                        vwap                   = vwap,
                        openPrice              = openPrice,
                        highPrice              = highPrice,
                        lowPrice               = lowPrice,
                        accumulatedVolume      = accumulatedVolume,
                        todaysChange           = todaysChange,
                        todaysChangePercentage = todaysChangePercentage,
                        previousDayVolume      = previousDayVolume,
                        previousDayClose       = previousDayClose,
                        previousDayOpen        = previousDayOpen,
                        previousDayHigh        = previousDayHigh,
                        previousDayLow         = previousDayLow,
                        hour                   = mockExecutionWindowPicker1.window[0],
                        minute                 = mockExecutionWindowPicker1.window[1],
                        externalTime           = 1605192894630916600,
                        creationDate           = date
                    )
        )
        DeltasOfStockIndicators.validateEntry(
            date             = date,
            position         = 0,
                referenceEntry  = DeltasOfStockIndicators(
                    ticker                          = "BCAT",
                    priceChangePercent              = 0.048473097430933414,
                    volumeChangePercent             = 0.0,
                    vwapChangePercent               = -0.8862515040664104,
                    volumePriceDeltaRatio           = 0.0, // (volumeChangePercent / priceChangePercent ) * 100
                    volatilityEstimate              =  2.405002405002405,
                    priceDeltaVolatilityRatio       = (0.048473097430933414 / 2.405002405002405) * 100,
                    priceDeltaVolatilityDiff        = 0.048473097430933414 - 2.405002405002405,
                    runningVolatilityEstimate       = ((20.64 - 20.506)/ 20.64)*100,
                    todayPreviousVolatilityDelta    = ((((20.64 - 20.506)/ 20.64)*100)/2.405002405002405) * 100,
                    hour                            =  mockExecutionWindowPicker1.window[0],
                    minute                          =  mockExecutionWindowPicker1.window[1],
                    externalTime                    =  1605192894630916600,
                    creationDate                    =  date
                )
        )

        /** Now download it at "10:30" and validate the results.------------------------------**/
        val mockPolyGonWebService2              = MockPolyGonWebService()
        mockPolyGonWebService2.downloadResponse = getWebServiceData("/intra-day/download-2.json")
        mockPolyGonWebService2.marketOpen       = true
        val mockExecutionWindowPicker2          = MockExecutionWindowPicker()
        mockExecutionWindowPicker2.window       = arrayOf(10,30) //9:30 AM

        IntraDayStockRecord.downloadContinuously(
            date                     = date,
            intraDayMarketWebService = mockPolyGonWebService2,
            executionWindowPicker    = mockExecutionWindowPicker2
        )

        /** Values in download-2.json are double the values in download-1 **/
        IntraDayStockRecord.validateEntry(
            date            = date,
            position        = 1,
                referenceEntry = IntraDayStockRecord(
                    ticker                 = "BCAT",
                    price                  = price*2,
                    vwap                   = vwap*2,
                    openPrice              = openPrice*2,
                    highPrice              = highPrice*2,
                    lowPrice               = lowPrice*2,
                    accumulatedVolume      = accumulatedVolume*2,
                    todaysChange           = todaysChange*2,
                    todaysChangePercentage = todaysChangePercentage*2,
                    previousDayVolume      = previousDayVolume*2,
                    previousDayClose       = previousDayClose*2,
                    previousDayOpen        = previousDayOpen*2,
                    previousDayHigh        = previousDayHigh*2,
                    previousDayLow         = previousDayLow*2,
                    hour                   = mockExecutionWindowPicker2.window[0],
                    minute                 = mockExecutionWindowPicker2.window[1],
                    externalTime           = 1605192894630916600,
                    creationDate           = date
                )
        )

        val volatilityEstimate        = ((42.0 - 41.0) / 41.58)*100.0
        val runningVolatilityEstimate = ((41.28 - 41.012)/ 41.28)*100.0

        DeltasOfStockIndicators.validateEntry(
            date     = date,
            position = 1,
                referenceEntry  = DeltasOfStockIndicators(
                    ticker                       = "BCAT",
                    priceChangePercent           = 100.0,
                    volumeChangePercent          = 100.0,
                    vwapChangePercent            = 100.0,
                    volumePriceDeltaRatio        = 100.0,
                    volatilityEstimate           = volatilityEstimate,
                    priceDeltaVolatilityRatio    = 4158.0,
                    priceDeltaVolatilityDiff     = 100.0 - 2.405002405002405,
                    runningVolatilityEstimate    = runningVolatilityEstimate,
                    todayPreviousVolatilityDelta = (runningVolatilityEstimate / volatilityEstimate) *100,
                    hour                         = mockExecutionWindowPicker2.window[0],
                    minute                       = mockExecutionWindowPicker2.window[1],
                    externalTime                 = 1605192894630916600,
                    creationDate                 = date
                )
        )
    }

    @Test
    fun will_not_execute_outside_of_valid_Windows() {
        val mockPolyGonWebService1              = MockPolyGonWebService()
        mockPolyGonWebService1.downloadResponse = getWebServiceData("/intra-day/download-1.json")
        mockPolyGonWebService1.marketOpen       = true
        val mockExecutionWindowPicker1          = MockExecutionWindowPicker()
        mockExecutionWindowPicker1.window       = arrayOf(9,0) //9:30 AM
        val date                                = Date(System.currentTimeMillis())

        /** Download one file at "9:30" and validate results followed by one at 10: 30 **/
        IntraDayStockRecord.downloadContinuously(
            date                     = date,
            intraDayMarketWebService = mockPolyGonWebService1,
            executionWindowPicker    = mockExecutionWindowPicker1
        )
        Assertions.assertFalse(IntraDayStockRecord.entriesExistForDate(date))
    }

    @Test
    fun download_can_process_closing_bell() {
        val mockPolyGonWebService1              = MockPolyGonWebService()
        mockPolyGonWebService1.downloadResponse = getWebServiceData("/intra-day/download-1.json")
        mockPolyGonWebService1.marketOpen       = true
        val mockExecutionWindowPicker1          = MockExecutionWindowPicker()
        mockExecutionWindowPicker1.window       = arrayOf(16,0) //9:30 AM
        val date                                = Date(System.currentTimeMillis()*3)

        /** Download one file at "9:30" and validate results followed by one at 10: 30 **/
        IntraDayStockRecord.downloadContinuously(
            date                     = date,
            intraDayMarketWebService = mockPolyGonWebService1,
            executionWindowPicker    = mockExecutionWindowPicker1
        )
        Assertions.assertTrue(IntraDayStockRecord.entriesExistForDate(date))
    }

    @Test
    fun download_will_not_run_if_market_is_closed() {
        val mockPolyGonWebService1              = MockPolyGonWebService()
        mockPolyGonWebService1.downloadResponse = getWebServiceData("/intra-day/download-1.json")
        mockPolyGonWebService1.marketOpen       = false
        val mockExecutionWindowPicker1          = MockExecutionWindowPicker()
        mockExecutionWindowPicker1.window       = arrayOf(9,30) //9:30 AM
        val date                                = Date(System.currentTimeMillis()*3)

        IntraDayStockRecord.downloadContinuously(
            date                     = date,
            intraDayMarketWebService = mockPolyGonWebService1,
            executionWindowPicker    = mockExecutionWindowPicker1
        )
        Assertions.assertFalse(IntraDayStockRecord.entriesExistForDate(date))
    }

    @Test
    fun download_will_save_new_stock_record_if_previous_is_missed_but_no_new_delta_until_next_download() {
        val mockPolyGonWebService1              = MockPolyGonWebService()
        mockPolyGonWebService1.downloadResponse = getWebServiceData("/intra-day/download-1.json")
        mockPolyGonWebService1.marketOpen       = true
        val mockExecutionWindowPicker1          = MockExecutionWindowPicker()
        mockExecutionWindowPicker1.window       = arrayOf(10,30) //9:30 AM
        val date                                = Date(System.currentTimeMillis()*3)

        IntraDayStockRecord.downloadContinuously(
            date                     = date,
            intraDayMarketWebService = mockPolyGonWebService1,
            executionWindowPicker    = mockExecutionWindowPicker1
        )
        Assertions.assertTrue(IntraDayStockRecord.entriesExistForDate(date))
        Assertions.assertFalse(DeltasOfStockIndicators.entriesExistForDate(date))

        /** Stock record was saved but delta was skipped. Now move to the next hour and confirm a delta is present
         *  because the new hour has a previous block in front of it.
         *  **/
        val mockPolyGonWebService2              = MockPolyGonWebService()
        mockPolyGonWebService2.downloadResponse = getWebServiceData("/intra-day/download-1.json")
        mockPolyGonWebService1.marketOpen       = false
        val mockExecutionWindowPicker2          = MockExecutionWindowPicker()
        mockExecutionWindowPicker2.window       = arrayOf(11,30) //9:30 AM

        IntraDayStockRecord.downloadContinuously(
            date                     = date,
            intraDayMarketWebService = mockPolyGonWebService2,
            executionWindowPicker    = mockExecutionWindowPicker2
        )
        Assertions.assertTrue(DeltasOfStockIndicators.entriesExistForDate(date))
    }

    private fun getWebServiceData(filename: String) : JSONObject{
        val fileContent = IntraDayStockRecordIntegrationTest::class.java.getResource(filename)!!.readText()
        val parser      = JSONParser()
        return parser.parse(fileContent) as JSONObject
    }

    class MockExecutionWindowPicker : ExecutionWindowPicker {
        lateinit var window : Array<Int> 
        override fun getExecutionWindow(): Array<Int> {
            println("Mock execution window running ${window[0]}:${window[1]}")
            return window
        }
    }

    class MockPolyGonWebService : IntraDayMarketWebService {
        var marketOpen       = true
        var downloadResponse = JSONObject()

        override fun downloadRecords(): JSONObject {
            println("Mock Polygon webservice download called")
            return this.downloadResponse
        }

        override fun isMarketOpen(): Boolean {
            println("Mock Polygon webservice market status called")
            return marketOpen
        }
    }
}