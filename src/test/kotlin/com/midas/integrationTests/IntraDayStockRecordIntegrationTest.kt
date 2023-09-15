package com.midas.integrationTests

import com.midas.domain.DeltasOfStockIndicators
import com.midas.domain.IntraDayStockRecord
import com.midas.interfaces.IntraDayMarketWebService
import com.midas.interfaces.ExecutionWindowPicker
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.junit.Assert
import org.junit.jupiter.api.AfterEach
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
        IntraDayStockRecord.deleteAll()
    }

    @Test
    fun download_can_parse_opening_bell_and_first_hour() {
        /*setup the mock services */
        val mockPolyGonWebService1              = MockPolyGonWebService()
        mockPolyGonWebService1.downloadResponse = getWebServiceData("/intra-day/download-1.json")
        mockPolyGonWebService1.marketOpen       = true
        val mockExecutionWindowPicker1          = MockExecutionWindowPicker()
        mockExecutionWindowPicker1.window       = arrayOf(9,30) //9:30 AM
        val date                                = Date(0)

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
                        price                  = 20.506,
                        vwap                   = 20.5105,
                        openPrice              = 20.64,
                        accumulatedVolume      = 37216,
                        todaysChange           = -0.124,
                        todaysChangePercentage = -0.601,
                        previousDayVolume      = 292738,
                        previousDayClose       = 20.63,
                        previousDayOpen        = 20.79,
                        previousDayHigh        = 21.0,
                        previousDayLow         = 20.5,
                        hour                   = mockExecutionWindowPicker1.window!![0],
                        minute                 = mockExecutionWindowPicker1.window!![1],
                        externalTime           = 1605192894630916600,
                        creationDate           = date
                    )
        )
        DeltasOfStockIndicators.validateEntry(
            date             = date,
            position         = 0,
                referenceEntry  = DeltasOfStockIndicators(
                    ticker              = "BCAT",
                    priceChangePercent  = 0.048473097430933414,
                    volumeChangePercent = 0.0,
                    vwapChangePercent   = -0.8862515040664104,
                    volatilityEstimate  =  2.405002405002405,
                    hour                =  mockExecutionWindowPicker1.window!![0],
                    minute              =  mockExecutionWindowPicker1.window!![1],
                    externalTime        =  1605192894630916600,
                    creationDate        =  date
                )
        )

        /** Now download it at "10:30" and validate the results.------------------------------**/
        val mockPolyGonWebService2              = MockPolyGonWebService()
        mockPolyGonWebService2.downloadResponse = getWebServiceData("/intra-day/download-1.json")
        mockPolyGonWebService2.marketOpen       = true
        val mockExecutionWindowPicker2          = MockExecutionWindowPicker()
        mockExecutionWindowPicker2.window       = arrayOf(10,30) //9:30 AM

        IntraDayStockRecord.downloadContinuously(
            date                     = date,
            intraDayMarketWebService = mockPolyGonWebService2,
            executionWindowPicker    = mockExecutionWindowPicker2
        )

        IntraDayStockRecord.validateEntry(
            date            = date,
            position        = 1,
                referenceEntry = IntraDayStockRecord(
                    ticker                 = "BCAT",
                    price                  = 20.506,
                    vwap                   = 20.5105,
                    openPrice              = 20.64,
                    accumulatedVolume      = 37216,
                    todaysChange           = -0.124,
                    todaysChangePercentage = -0.601,
                    previousDayVolume      = 292738,
                    previousDayClose       = 20.63,
                    previousDayOpen        = 20.79,
                    previousDayHigh        = 21.0,
                    previousDayLow         = 20.5,
                    hour                   = mockExecutionWindowPicker2.window!![0],
                    minute                 = mockExecutionWindowPicker2.window!![1],
                    externalTime           = 1605192894630916600,
                    creationDate           = date
                )
        )
        DeltasOfStockIndicators.validateEntry(
            date     = date,
            position = 1,
                referenceEntry  = DeltasOfStockIndicators(
                    ticker              = "BCAT",
                    priceChangePercent  = 0.0,
                    volumeChangePercent = 0.0,
                    vwapChangePercent   = 0.0,
                    volatilityEstimate  =  2.405002405002405,
                    hour                =  mockExecutionWindowPicker2.window!![0],
                    minute              =  mockExecutionWindowPicker2.window!![1],
                    externalTime        =  1605192894630916600,
                    creationDate        =  date
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
        Assert.assertFalse(IntraDayStockRecord.entriesExistForDate(date))
    }

    @Test
    fun download_can_process_closing_bell() {
        /*setup the mock services */
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
        Assert.assertTrue(IntraDayStockRecord.entriesExistForDate(date))
    }

    @Test
    fun download_will_not_run_if_market_is_closed() {
        /*setup the mock services */
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
        Assert.assertFalse(IntraDayStockRecord.entriesExistForDate(date))
    }

    @Test
    fun download_will_save_new_stock_record_if_previous_is_missed_but_no_new_delta_until_next_download() {
        /*setup the mock services */
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
        Assert.assertTrue(IntraDayStockRecord.entriesExistForDate(date))
        Assert.assertFalse(DeltasOfStockIndicators.entriesExistForDate(date))

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
        Assert.assertTrue(DeltasOfStockIndicators.entriesExistForDate(date))
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