package com.midas.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app")
class ApplicationProperties {
    val runIntraDayStockService: Boolean
    val polygonMarketStatusURL : String
    val polygonAllTickersURL   : String
    val polyGonApiKey          : String
    val selectedHbmDialect     : String
    val errorDirectory         : String
    val isNotIntegrationTest      : Boolean
    val pollIntervalMins       : Int

    constructor(
            runIntraDayStockService: Boolean,
            polygonMarketStatusURL : String,
            polygonAllTickersURL   : String,
            polyGonApiKey          : String,
            selectedHbmDialect     : String,
            errorDirectory         : String,
            isNotIntegrationTest   : Boolean,
            pollIntervalMins       : Int
    ) {
        this.runIntraDayStockService = runIntraDayStockService
        this.polygonMarketStatusURL  = polygonMarketStatusURL
        this.polygonAllTickersURL    = polygonAllTickersURL
        this.polyGonApiKey           = polyGonApiKey
        this.selectedHbmDialect      = selectedHbmDialect
        this.errorDirectory          = errorDirectory
        this.isNotIntegrationTest    = isNotIntegrationTest
        this.pollIntervalMins        = pollIntervalMins
    }
}