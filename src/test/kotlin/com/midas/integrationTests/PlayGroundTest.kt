package com.midas.integrationTests

import org.junit.jupiter.api.Test

class PlayGroundTest {
    @Test
    fun runIt() {
        var a = 0
        var b = 0
        Thread {
            while(true) {
                println("Child thread A")
                Thread.sleep(1000)
                a += 2
            }
        }.start()
        Thread {
            while(true) {
                println("Child thread B")
                Thread.sleep(1000)
                b += 4
            }
        }.start()
        println("Main thread exiting...")
        var i = 0
        while (i < 10) {
            println("a: $a, b: $b")
            Thread.sleep(1000)
            i++
        }
    }
}