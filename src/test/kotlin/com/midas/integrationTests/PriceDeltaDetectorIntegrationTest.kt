package com.midas.integrationTests

import com.midas.domain.PriceDeltaDetector
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PriceDeltaDetectorIntegrationTest {
    @BeforeEach
    fun init() {
        PriceDeltaDetector.windowSizesForTestMode = true
        PriceDeltaDetector.clearAllDataOnlyForIntegrationTests()
    }

    @Order(0)
    @Test
    fun Can_rank_record_breaking_positive_deltas() {
        val ticker                = "AAPL"
        val newPriceDelta         = 50.0
        val price                 = 1.0 + (newPriceDelta/100.0)
        val distance              = 11 // distance from the last spike
        val initialPopulationSize = 19

        /** Create a population of 10,000 pts or 10,000 minutes in this case **/
        val population: Array<Double> = Array(initialPopulationSize) {1.0}

        /** Set a spike that the new spike will overtake **/
        population[distance] = (1.0 + (25.0/100.0))

        /** Populate the ranker **/
        for(i in population.indices) {
            val result = PriceDeltaDetector.rank(
                date   =  Date(System.currentTimeMillis()),
                stocks = listOf(Pair(ticker,population[i]))
            )
            println("pt: $i, rankings: ${result.size}")
            if (result.isNotEmpty()) {
                for(r in result) {
                    println("timeWindow: ${r.timeWindow}, distance: ${r.distance}, delta: ${r.priceDelta}")
                }
            }
        }

        /** Rank the deltas in the population **/
        val rankings: List<PriceDeltaDetector.PriceChangeMilestone> = PriceDeltaDetector.rank(
            date   =  Date(System.currentTimeMillis()),
            stocks = listOf(Pair(ticker,price))
        ).sortedByDescending { it.probabilityCoefficient }

        println("Rankings:")
        rankings.forEach {
            println("r -> ticker: ${it.ticker}, priceDelta: ${it.priceDelta}, offset: ${it.offset} distance: ${it.distance}, timeWindow: ${it.timeWindow}, milestoneDelta: ${it.milestoneChange}")
        }

        /** With initialPopulationSize = 19 the only time windows are 2,5,10, and 15
         * For the placement of the test spikes the deltas correspond the following arbitrary values
         * **/
        val ranking1 : PriceDeltaDetector.PriceChangeMilestone = rankings[0]
        Assertions.assertEquals(19, ranking1.distance)
        Assertions.assertEquals(newPriceDelta, ranking1.priceDelta)
        Assertions.assertEquals(20, ranking1.timeWindow)
        Assertions.assertEquals(ticker, ranking1.ticker)

        /** Now add another point that suddenly jumps 200% from the previous. Because that will make N 21 points long the 20 pt timewindow should kick in as well as a 2 pt of the
         * large jump **/
        val rankings2: List<PriceDeltaDetector.PriceChangeMilestone> = PriceDeltaDetector.rank(
            date   =  Date(System.currentTimeMillis()),
            stocks = listOf(Pair(ticker,4.50))
        ).sortedByDescending { it.probabilityCoefficient }

        println("Rankings 2")
        rankings2.forEach {
            println("r -> ticker: ${it.ticker}, priceDelta: ${it.priceDelta}, distance: ${it.distance}, timeWindow: ${it.timeWindow}, milestoneDelta: ${it.milestoneChange}")
        }

        Assertions.assertEquals(11, rankings2[0].distance)
        Assertions.assertEquals(260.0, rankings2[0].priceDelta)
        Assertions.assertEquals(10, rankings2[0].timeWindow)
        Assertions.assertEquals(ticker, rankings2[0].ticker)

        /** Ensure the order is correct by probability coefficient (timeWindow * distance). Important to note that
         * the priceDelta is not included in the sort. **/
        Assertions.assertEquals(10, rankings2[0].timeWindow)
        Assertions.assertEquals(15, rankings2[1].timeWindow)
        Assertions.assertEquals(5, rankings2[2].timeWindow)
        Assertions.assertEquals(2, rankings2[3].timeWindow)
        Assertions.assertEquals(20, rankings2[4].timeWindow)
        Assertions.assertEquals(5, rankings2.size)
    }

    @Order(1)
    @Test
    fun Will_not_rank_negative_deltas() {
        /** Create a population for AAPL**/
        val populationApple: Array<Double> = Array(14) {1.0}
        populationApple[1] = 0.05

        /** Populate the ranker and assert it records no negative deltas **/
        for(i in populationApple.indices) {
            val result = PriceDeltaDetector.rank(
                date   =  Date(System.currentTimeMillis()),
                stocks = listOf(Pair("AAPL",populationApple[i]))
            )
            println("pt: $i, rankings: ${result.size}")
            if (result.isNotEmpty()) {
                for(r in result) {
                    Assertions.assertTrue(r.priceDelta >= 0)
                }
            }
        }
    }

    @Order(2)
    @Test
    fun Can_rank_with_multiple_tickers_on_larger_data_sets() {
        PriceDeltaDetector.windowSizesForTestMode = false

        val start = System.currentTimeMillis()

        /** Create a population for AAPL**/
        val populationApple: Array<Double> = Array(10000) {1.0}
        populationApple[1] = 12.0

        /** Create a population for TSLA **/
        val populationTesla: Array<Double> = Array(10000) {1.0}
        populationTesla[1] = 12.0

        /** Populate the ranker and assert it records no negative deltas **/
        for(i in populationApple.indices) {
            println("insert: $i")
            val result = PriceDeltaDetector.rank(
                date   =  Date(System.currentTimeMillis()),
                stocks = listOf(Pair("AAPL",populationApple[i]),Pair("TSLA",populationTesla[i]))
            )
            if (result.isNotEmpty()) {
                for(r in result) {
                    Assertions.assertTrue(r.priceDelta >= 0)
                }
            }
        }
        /*val results = PriceDeltaDetector.rank(
            date   =  Date(System.currentTimeMillis()),
            stocks = listOf(Pair("AAPL",100.0),Pair("TSLA",100.0))
        ).sortedByDescending { it.probabilityCoefficient }

        for(i in 0 until 13) {
            Assertions.assertEquals(results[(i*2) + 1].timeWindow, results[i*2].timeWindow)
            Assertions.assertEquals(results[(i*2) + 1].priceDelta, results[i*2].priceDelta)
            Assertions.assertEquals(results[(i*2) + 1].distance, results[i*2].distance)
            Assertions.assertEquals(results[(i*2) + 1].probabilityCoefficient, results[i*2].probabilityCoefficient)
            Assertions.assertNotEquals(results[(i*2) + 1].ticker, results[i*2].ticker)
        }
        Assertions.assertEquals(26, results.size)*/
        println("X: " + ((System.currentTimeMillis() - start)/1000))
        //fail("Done")
    }

    @Order(3)
    @Test
    fun Will_not_rank_zero_prices() {
        /** Create a population for AAPL**/
        val populationApple: Array<Double> = Array(14) {0.0}
        populationApple[1] = 0.0

        /** Populate the ranker and assert it records no negative deltas **/
        for(i in populationApple.indices) {
            val result = PriceDeltaDetector.rank(
                date   =  Date(System.currentTimeMillis()),
                stocks = listOf(Pair("AAPL",populationApple[i]))
            )
            Assertions.assertTrue(result.isEmpty())
            println("pt: $i, rankings: ${result.size}")
        }
    }

    @Order(4)
    @Test
    fun can_process_multiple_tickers_with_varying_milestones_each() {
        val tickerA = "AAPL"
        val tickerB = "TSLA"

        /** AAPL -> 1,2
         * TSLA  -> 1,3
         * **/
        PriceDeltaDetector.rank(
            date   =  Date(System.currentTimeMillis()),
            stocks = listOf(Pair(tickerA,1.0),Pair(tickerB,1.0))
        )
        val result = PriceDeltaDetector.rank(
            date   =  Date(System.currentTimeMillis()),
            stocks = listOf(Pair(tickerA,2.0),Pair(tickerB,3.0))
        )
        for(r in result) {
            println("r0 -> $r")
        }
        println("")

        Assertions.assertTrue(result.isNotEmpty())
        Assertions.assertEquals(tickerA, result[0].ticker)
        Assertions.assertEquals(100.0, result[0].priceDelta)
        Assertions.assertEquals(1, result[0].distance)
        Assertions.assertEquals(2, result[0].timeWindow)
        Assertions.assertEquals(1, result[0].offset)
        Assertions.assertEquals(2.0, result[0].price)
        Assertions.assertEquals(0.0, result[0].milestoneChange)

        Assertions.assertTrue(result.isNotEmpty())
        Assertions.assertEquals(tickerB, result[1].ticker)
        Assertions.assertEquals(200.0, result[1].priceDelta)
        Assertions.assertEquals(1, result[1].distance)
        Assertions.assertEquals(2, result[1].timeWindow)
        Assertions.assertEquals(1, result[1].offset)
        Assertions.assertEquals(3.0, result[1].price)
        Assertions.assertEquals(0.0, result[1].milestoneChange)
        println("")

        /** AAPL -> 1,2,7
         * TSLA  -> 1,3,7
         * **/
         var result2:List<PriceDeltaDetector.PriceChangeMilestone> = PriceDeltaDetector.rank(
            date   =  Date(System.currentTimeMillis()),
            stocks = listOf(Pair(tickerA, 7.0), Pair(tickerB, 7.0))
        )
        println("r2(0) -> ${result2[0]}")
        //println("r2(1) -> ${result2[1]}")
        /** Only AAPL gets a w(2) milestone because the jump from 3 - 7 for TSLA is less than the jump from 1 to 3 **/
        Assertions.assertEquals(1L, result2[0].distance)
        Assertions.assertEquals("AAPL", result2[0].ticker)
        Assertions.assertEquals(1, result2.size)
        println("")

        /** AAPL -> 1,2,7,7
         * TSLA  -> 1,3,7,7
         * **/
        result2 = PriceDeltaDetector.rank(
            date   =  Date(System.currentTimeMillis()),
            stocks = listOf(Pair(tickerA, 7.0), Pair(tickerB, 7.0))
        )
        Assertions.assertTrue(result2.isEmpty())

        /** AAPL -> 1,2,7,7,7
         * TSLA  -> 1,3,7,7,7
         * **/
        result2 = PriceDeltaDetector.rank(
            date   =  Date(System.currentTimeMillis()),
            stocks = listOf(Pair(tickerA, 7.0), Pair(tickerB, 7.0))
        )
        println("r2(0) -> ${result2[0]}")
        println("r2(1) -> ${result2[1]}")
        Assertions.assertTrue(result2[0].timeWindow == 5)
        Assertions.assertTrue(result2[0].offset == 4L)
        Assertions.assertTrue(result2[1].timeWindow == 5)
        Assertions.assertTrue(result2[1].offset == 4L)
        println("")

        /** AAPL -> 1,2,7,7,7,14
         * TSLA  -> 1,3,7,7,7,14
         * **/
        val result3 = PriceDeltaDetector.rank(
            date   =  Date(System.currentTimeMillis()),
            stocks = listOf(Pair(tickerA, 14.0), Pair(tickerB, 14.0))
        )
        /** The jump to 14 is 100% for window = 5 but the previous window=5 had a jump of 1-7 so its already taken **/
        Assertions.assertTrue(result3.isEmpty())

        /** AAPL -> 1,2,7,7,7,14,1,1,1
         * TSLA  -> 1,3,7,7,7,14,2,2,2
         * **/
        repeat(3) {
            Assertions.assertTrue(PriceDeltaDetector.rank(
                date   =  Date(System.currentTimeMillis()),
                stocks = listOf(Pair(tickerA, 1.0), Pair(tickerB, 2.0))
            ).isEmpty())
        }

        /** AAPL -> 1,2,7,7,7,14,1,1,1
         * TSLA  -> 1,3,7,7,7,14,2,2,2
         * **/
        val resultg = PriceDeltaDetector.rank(
            date   =  Date(System.currentTimeMillis()),
                stocks = listOf(Pair(tickerA, 1.0), Pair(tickerB, 2.0))
            )
        println("resultg[0]: ${resultg[0]}")
        println("resultg[1]: ${resultg[1]}")
        Assertions.assertEquals(10,resultg[0].timeWindow)
        Assertions.assertEquals(9L,resultg[0].distance)
        Assertions.assertEquals(9L,resultg[0].offset)
        Assertions.assertEquals(1.0,resultg[0].price)
        Assertions.assertEquals(0.0,resultg[0].priceDelta)
        Assertions.assertEquals(0.0,resultg[0].milestoneChange)

        Assertions.assertEquals(10,resultg[1].timeWindow)
        Assertions.assertEquals(9,resultg[1].distance)
        Assertions.assertEquals(9,resultg[1].offset)
        Assertions.assertEquals(2.0,resultg[1].price)
        Assertions.assertEquals(100.0,resultg[1].priceDelta)
        Assertions.assertEquals(9,resultg[1].distance)
        Assertions.assertEquals(0.0,resultg[1].milestoneChange)

        /** AAPL -> 1,2,7,7,7,14,1,1,1,1,70
         * TSLA  -> 1,3,7,7,7,14,2,2,2,2,98
         * **/
        println("")
        val result4 = PriceDeltaDetector.rank(
            date   =  Date(System.currentTimeMillis()),
            stocks = listOf(Pair(tickerA, 14.0*5), Pair(tickerB, 14.0*7))
        )
        println("result4: ${result4.size}")
        for(i in result4.indices) {
            println("result4[$i] -> ${result4[i]}")
        }

        Assertions.assertEquals(10,result4[2].timeWindow)
        Assertions.assertEquals(1L,result4[2].distance)
        Assertions.assertEquals(9L,result4[2].offset)
        Assertions.assertEquals(70.0,result4[2].price)
        Assertions.assertEquals(3400.0,result4[2].priceDelta)
        Assertions.assertEquals(0.0,result4[2].milestoneChange)

        Assertions.assertEquals(10,result4[5].timeWindow)
        Assertions.assertEquals(1,result4[5].distance)
        Assertions.assertEquals(9,result4[5].offset)
        Assertions.assertEquals(98.0,result4[5].price)
        Assertions.assertEquals(3166.666666666667,result4[5].priceDelta)
        Assertions.assertEquals(3066.666666666667,result4[5].milestoneChange)
    }
}