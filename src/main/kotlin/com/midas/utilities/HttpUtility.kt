package com.midas.utilities

import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by Usman Mutawakil on 10/20/20.
 */
class HttpUtility {
    companion object {
        fun getJSONObject(inputURL: String): JSONObject {
            return (JSONParser().parse(get(inputURL = inputURL)) as JSONObject)
        }

        fun get(inputURL: String): String {
            val httpURLConnection: HttpURLConnection = URL(inputURL ).openConnection() as HttpURLConnection
            return get(httpURLConnection)
        }

        fun get(httpURLConnection: HttpURLConnection): String {
            httpURLConnection.setRequestProperty("Content-Type", "application/json")
            httpURLConnection.requestMethod = "GET"

            val reader = BufferedReader(InputStreamReader(getStream(httpURLConnection)))
            val response: StringBuilder = StringBuilder("")
            var line: String? = reader.readLine()
            while (line != null) {
                response.append(line)
                line = reader.readLine()
            }
            if((httpURLConnection.responseCode >= 200) && (httpURLConnection.responseCode <= 299)) {
                //println("HttpURLConnection request succeeded -> HTTP Code: ${httpURLConnection.responseCode}")
                //println(response.toString())
                return response.toString()
            } else {
                println("HttpURLConnection request failed -> HTTP Code: ${httpURLConnection.responseCode}.")
                throw RuntimeException(response.toString())
            }
        }

        private fun getStream(httpURLConnection: HttpURLConnection) : InputStream {
            if(httpURLConnection.responseCode < 200 || httpURLConnection.responseCode > 299) {
                return httpURLConnection.errorStream
            } else {
                return httpURLConnection.inputStream
            }
        }
    }
}