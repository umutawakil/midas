package com.midas.integrationTests

import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.*


class PlayGroundTest {
    @Test
    fun runIt() {
        val formatter = SimpleDateFormat("MM-dd-yyyy HH:mm:ss", Locale.ENGLISH)
        val dateInString1 = "09-15-2023 10:30:00" // "08-15-2023 09:30:00" // "08-15-2023 09:30:00"
        val date1: Date = formatter.parse(dateInString1)
        println("S: " + date1.time * 1000000)

        val dateInString2 = "09-15-2023 10:30:00" // "08-15-2023 09:30:00" // "08-15-2023 09:30:00"
        val date2: Date = formatter.parse(dateInString2)
        println("S: " + date2.time * 1000000)

        println("X: " + 1694786443000 * 1000000)
    }
}