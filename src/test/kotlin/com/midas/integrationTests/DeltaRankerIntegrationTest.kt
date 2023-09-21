package com.midas.integrationTests

import com.midas.domain.DeltaRanker
import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DeltaRankerIntegrationTest {
    @Order(0)
    @Test
    fun Can_rank_record_breaking_positive_deltas() {
        val ticker                = "AAPL"
        val newPriceDelta         = 50.0
        val price                 = 1.0 + (newPriceDelta/100.0)
        val distance              = 10 // distance from the last spike
        val initialPopulationSize = 19

        /** Create a population of 10,000 pts or 10,000 minutes in this case **/
        val population: Array<Double> = Array(initialPopulationSize) {1.0}

        /** Set a spike that the new spike will overtake **/
        population[distance] = ( 1.0 + (25.0/100.0))

        /** Populate the ranker **/
        for(i in population.indices) {
            val result = DeltaRanker.rank(
                stocks = listOf(Pair(ticker,population[i]))
            )
            println("pt: $i, rankings: ${result.size}")
            if (result.size >  0) {
                for(r in result) {
                    println("timeWindow: ${r.timeWindow}, distance: ${r.distance}, delta: ${r.priceDelta}")
                }
            }
        }

        /** Rank the deltas in the population **/
        val rankings: List<DeltaRanker.DeltaRanking> = DeltaRanker.rank(
            stocks = listOf(Pair(ticker,price))
        ).sortedByDescending { it.propabilityCoefficient }

        println("Rankings:")
        rankings.forEach {
            println("r -> ticker: ${it.ticker}, priceDelta: ${it.priceDelta}, distance: ${it.distance}, timeWindow: ${it.timeWindow}, rankDelta: ${it.rankChangePercent}")
        }

        /** With initialPopulationSize = 19 the only time windows are 2,5,10, and 15
         * For the placement of the test spikes the deltas correspond the following arbitrary values
         * **/
        val ranking1 : DeltaRanker.DeltaRanking = rankings[0]
        Assertions.assertEquals(9, ranking1.distance)
        Assertions.assertEquals(newPriceDelta, ranking1.priceDelta)
        Assertions.assertEquals(10, ranking1.timeWindow)
        Assertions.assertEquals(ticker, ranking1.ticker)

        /** Now add another point that suddenly jumps 200% from the previous. Because that will make N 21 points long the 20 pt timewindow should kick in as well as a 2 pt of the
         * large jump **/
        val rankings2: List<DeltaRanker.DeltaRanking> = DeltaRanker.rank(stocks = listOf(Pair(ticker,4.50))).sortedByDescending { it.propabilityCoefficient }

        println("Rankings 2")
        rankings.forEach {
            println("r -> ticker: ${it.ticker}, priceDelta: ${it.priceDelta}, distance: ${it.distance}, timeWindow: ${it.timeWindow}, rankDelta: ${it.rankChangePercent}")
        }

        Assertions.assertEquals(20, rankings2[0].distance)
        Assertions.assertEquals(350.0, rankings2[0].priceDelta)
        Assertions.assertEquals(20, rankings2[0].timeWindow)
        Assertions.assertEquals(ticker, rankings2[0].ticker)

        /** Ensure the order is correct by probability coefficient (timeWindow * distance). Important to note that
         * the priceDelta is not included in the sort. **/
        Assertions.assertEquals(20, rankings2[0].timeWindow)
        Assertions.assertEquals(10, rankings2[1].timeWindow)
        Assertions.assertEquals(5, rankings2[2].timeWindow)
        Assertions.assertEquals(15, rankings2[3].timeWindow)
        Assertions.assertEquals(1, rankings2[4].timeWindow)
        Assertions.assertEquals(5, rankings2.size)
    }

    @Order(1)
    @Test
    fun Will_not_rank_negative_deltas() {
        DeltaRanker.clearAllDataOnlyForIntegrationTests()

        /** Create a population for AAPL**/
        val populationApple: Array<Double> = Array(14) {1.0}
        populationApple[1] = 0.05

        /** Populate the ranker and assert it records no negative deltas **/
        for(i in populationApple.indices) {
            val result = DeltaRanker.rank(
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
    fun Can_rank_with_multiple_tickers() {
        DeltaRanker.clearAllDataOnlyForIntegrationTests()

        /** Create a population for AAPL**/
        val populationApple: Array<Double> = Array(14) {1.0}
        populationApple[1] = 12.0

        /** Create a population for TSLA **/
        val populationTesla: Array<Double> = Array(14) {1.0}
        populationTesla[1] = 12.0

        /** Populate the ranker and assert it records no negative deltas **/
        for(i in populationApple.indices) {
            val result = DeltaRanker.rank(
                stocks = listOf(Pair("AAPL",populationApple[i]),Pair("TSLA",populationTesla[i]))
            )
            println("pt: $i, rankings: ${result.size}")
            if (result.isNotEmpty()) {
                for(r in result) {
                    Assertions.assertTrue(r.priceDelta >= 0)
                }
            }
        }
        val results = DeltaRanker.rank(
            stocks = listOf(Pair("AAPL",100.0),Pair("TSLA",100.0))
        ).sortedByDescending { it.propabilityCoefficient }

        for (r in results) {
            println("ticker: ${r.ticker}, delta: ${r.priceDelta}, probability: ${r.propabilityCoefficient}, window: ${r.timeWindow}, distance: ${r.distance}")
        }

        for(i in 0 until 3) {
            Assertions.assertEquals(results[(i*2) + 1].timeWindow, results[i*2].timeWindow)
            Assertions.assertEquals(results[(i*2) + 1].priceDelta, results[i*2].priceDelta)
            Assertions.assertEquals(results[(i*2) + 1].distance, results[i*2].distance)
            Assertions.assertEquals(results[(i*2) + 1].propabilityCoefficient, results[i*2].propabilityCoefficient)
            Assertions.assertNotEquals(results[(i*2) + 1].ticker, results[i*2].ticker)
        }
        Assertions.assertEquals(6, results.size)
    }

    @Order(3)
    @Test
    fun Can_rank_with_multiple_tickers_on_larger_data_sets() {
        DeltaRanker.clearAllDataOnlyForIntegrationTests()

        /** Create a population for AAPL**/
        val populationApple: Array<Double> = Array(10000) {1.0}
        populationApple[1] = 12.0

        /** Create a population for TSLA **/
        val populationTesla: Array<Double> = Array(10000) {1.0}
        populationTesla[1] = 12.0

        /** Populate the ranker and assert it records no negative deltas **/
        for(i in populationApple.indices) {
            val result = DeltaRanker.rank(
                stocks = listOf(Pair("AAPL",populationApple[i]),Pair("TSLA",populationTesla[i]))
            )
            if (result.isNotEmpty()) {
                for(r in result) {
                    Assertions.assertTrue(r.priceDelta >= 0)
                }
            }
        }
        val results = DeltaRanker.rank(
            stocks = listOf(Pair("AAPL",100.0),Pair("TSLA",100.0))
        ).sortedByDescending { it.propabilityCoefficient }

        for(i in 0 until 13) {
            Assertions.assertEquals(results[(i*2) + 1].timeWindow, results[i*2].timeWindow)
            Assertions.assertEquals(results[(i*2) + 1].priceDelta, results[i*2].priceDelta)
            Assertions.assertEquals(results[(i*2) + 1].distance, results[i*2].distance)
            Assertions.assertEquals(results[(i*2) + 1].propabilityCoefficient, results[i*2].propabilityCoefficient)
            Assertions.assertNotEquals(results[(i*2) + 1].ticker, results[i*2].ticker)
        }
        Assertions.assertEquals(26, results.size)
    }

    @Order(4)
    @Test
    fun Will_not_rank_zero_prices() {
        DeltaRanker.clearAllDataOnlyForIntegrationTests()

        /** Create a population for AAPL**/
        val populationApple: Array<Double> = Array(14) {0.0}
        populationApple[1] = 0.0

        /** Populate the ranker and assert it records no negative deltas **/
        for(i in populationApple.indices) {
            val result = DeltaRanker.rank(
                stocks = listOf(Pair("AAPL",populationApple[i]))
            )
            Assertions.assertTrue(result.isEmpty())
            println("pt: $i, rankings: ${result.size}")
        }
    }
}