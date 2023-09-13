package com.midas.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app")
class ApplicationProperties {
    val runIntraDayStockService: Boolean
    val polygonMarketStatusURL : String
    val polygonAllTickersURL   : String
    val polyGonApiKey          : String
    val selectedHbmDialect     : String
    constructor(
        runIntraDayStockService: Boolean,
        polygonMarketStatusURL : String,
        polygonAllTickersURL   : String,
        polyGonApiKey          : String,
        selectedHbmDialect     : String
    ) {
        this.runIntraDayStockService = runIntraDayStockService
        this.polygonMarketStatusURL  = polygonMarketStatusURL
        this.polygonAllTickersURL    = polygonAllTickersURL
        this.polyGonApiKey           = polyGonApiKey
        this.selectedHbmDialect      = selectedHbmDialect
    }
}