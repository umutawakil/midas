package com.midas.interfaces

import org.json.simple.JSONObject

interface IntraDayMarketWebService {
    fun downloadRecords() : JSONObject
    fun isMarketOpen(): Boolean
}