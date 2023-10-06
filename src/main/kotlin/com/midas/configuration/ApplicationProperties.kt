package com.midas.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app")
class ApplicationProperties {
    val runIntraDayStockService: Boolean
    val polygonBaseUrl         : String
    val polygonMarketStatusURL : String
    val polygonAllTickersURL   : String
    val polyGonApiKey          : String
    val selectedHbmDialect     : String
    val errorDirectory         : String
    val isNotIntegrationTest   : Boolean
    val pollIntervalMins       : Int
    val runStockSnapshotImport : Boolean

    constructor(
            runIntraDayStockService: Boolean,
            polygonBaseUrl         : String,
            polygonMarketStatusURL : String,
            polygonAllTickersURL   : String,
            polyGonApiKey          : String,
            selectedHbmDialect     : String,
            errorDirectory         : String,
            isNotIntegrationTest   : Boolean,
            pollIntervalMins       : Int,
            runStockSnapshotImport : Boolean
    ) {
        this.runIntraDayStockService = runIntraDayStockService
        this.polygonBaseUrl          = polygonBaseUrl
        this.polygonMarketStatusURL  = polygonMarketStatusURL
        this.polygonAllTickersURL    = polygonAllTickersURL
        this.polyGonApiKey           = polyGonApiKey
        this.selectedHbmDialect      = selectedHbmDialect
        this.errorDirectory          = errorDirectory
        this.isNotIntegrationTest    = isNotIntegrationTest
        this.pollIntervalMins        = pollIntervalMins
        this.runStockSnapshotImport  = runStockSnapshotImport
    }
}