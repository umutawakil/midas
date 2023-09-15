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
    val isIntegrationTest      : Boolean

    constructor(
            runIntraDayStockService: Boolean,
            polygonMarketStatusURL : String,
            polygonAllTickersURL   : String,
            polyGonApiKey          : String,
            selectedHbmDialect     : String,
            errorDirectory         : String,
            isIntegrationTest      : Boolean
    ) {
        this.runIntraDayStockService = runIntraDayStockService
        this.polygonMarketStatusURL  = polygonMarketStatusURL
        this.polygonAllTickersURL    = polygonAllTickersURL
        this.polyGonApiKey           = polyGonApiKey
        this.selectedHbmDialect      = selectedHbmDialect
        this.errorDirectory          = errorDirectory
        this.isIntegrationTest       = isIntegrationTest
    }
}