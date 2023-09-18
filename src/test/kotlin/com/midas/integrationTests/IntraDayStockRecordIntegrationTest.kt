package com.midas.integrationTests

import com.midas.domain.IntraDayStockRecord
import com.midas.interfaces.IntraDayMarketWebService
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

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
    fun download_twice() {
        val mockPolyGonWebService1              = MockPolyGonWebService()
        mockPolyGonWebService1.downloadResponse = getWebServiceData("/intra-day/download-1.json")
        IntraDayStockRecord.downloadContinuously(
            intraDayMarketWebService = mockPolyGonWebService1
        )

        val mockPolyGonWebService2              = MockPolyGonWebService()
        mockPolyGonWebService2.downloadResponse = getWebServiceData("/intra-day/download-2.json")
        IntraDayStockRecord.downloadContinuously(
            intraDayMarketWebService = mockPolyGonWebService2,
        )
    }

    private fun getWebServiceData(filename: String) : JSONObject{
        val fileContent = IntraDayStockRecordIntegrationTest::class.java.getResource(filename)!!.readText()
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