package com.midas.integrationTests

import com.midas.domain.StockSnapshot
import com.midas.interfaces.IntraDayMarketWebService
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class StockSnapshotIntegrationTest(
        @Autowired private val stockSnapshot: StockSnapshot
) {

    @BeforeEach
    fun setup() {
        println("Deleting intra day records...")
        StockSnapshot.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        StockSnapshot.deleteAll()
    }

    @Test
    fun download_twice() {
        val mockPolyGonWebService1              = MockPolyGonWebService()
        mockPolyGonWebService1.downloadResponse = getWebServiceData("/intra-day/download-1.json")
        StockSnapshot.downloadContinuously(
            intraDayMarketWebService = mockPolyGonWebService1
        )

        val mockPolyGonWebService2              = MockPolyGonWebService()
        mockPolyGonWebService2.downloadResponse = getWebServiceData("/intra-day/download-2.json")
        StockSnapshot.downloadContinuously(
            intraDayMarketWebService = mockPolyGonWebService2,
        )
    }

    private fun getWebServiceData(filename: String) : JSONObject{
        val fileContent = StockSnapshotIntegrationTest::class.java.getResource(filename)!!.readText()
        val parser      = JSONParser()
        return parser.parse(fileContent) as JSONObject
    }

    class MockPolyGonWebService : IntraDayMarketWebService {
        var downloadResponse = JSONObject()

        override fun downloadRecords(): JSONObject {
            println("Mock Polygon webservice download called")
            return this.downloadResponse
        }
    }
}