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
    val financialsDirectory    : String
    val awsSecretKey           : String
    val awsAccessKey           : String
    val deploymentBucket       : String

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
            financialsDirectory    : String,
            awsSecretKey           : String,
            awsAccessKey           : String,
            deploymentBucket       : String
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
        this.financialsDirectory     = financialsDirectory
        this.awsSecretKey            = awsSecretKey
        this.awsAccessKey            = awsAccessKey
        this.deploymentBucket        = deploymentBucket
    }
}