package com.midas.domain

import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class DeltaRanker {
    companion object {
        //TODO: ALl the core object classes need to be loaded from the database so the system can pick up
        //  where it left off
        // Needs a comparator for decreasing and magnitude

        private val latestRankings = mutableListOf<DeltaRanking>()
        private val tickerGroups   = mutableMapOf<String, TickerGroup>()

        //TODO: Needs to be initialized from the database or size of StockPriceInfo
        private var currentOffset = 0

        fun rank(stocks: List<Pair<String, Double>>): List<DeltaRanking> {
            latestRankings.clear()
            stocks.forEach {
                rank(
                    ticker = it.first,
                    price = it.second
                )
            }

            currentOffset++

            //latestRankings.sortByDescending { it.propabilityCoefficient }
            return latestRankings
        }

        private fun rank(ticker: String, price: Double) {
            if(price <=0) {
                return
            }

            val tickerGroup = tickerGroups.computeIfAbsent(
                ticker,
            ) {
                TickerGroup(ticker = ticker)
            }
            tickerGroup.rank(price = price)
        }

        /** If this ever makes it into production I'm sure it will be because of autocomplete **/
        fun clearAllDataOnlyForIntegrationTests() {
            latestRankings.clear()
            tickerGroups.clear()
            currentOffset = 0
        }
    }

    private class TickerGroup(ticker: String) {
        private val ticker: String
        private val windowGroups: MutableList<WindowGroup> = mutableListOf()

        init {
            this.ticker = ticker
            initWindowGroups()
        }

        fun initWindowGroups() {
            windowGroups.add(
                WindowGroup(ticker = ticker, size = 1)
            )
            for (i in 1 until 13) {
                windowGroups.add(
                    WindowGroup(ticker = ticker, size = 5 * i)
                )
            }
        }

        fun rank(price: Double) {
            windowGroups.forEach{ it.rank(price = price)}
        }
    }
    private class WindowGroup(ticker: String, size: Int) {
        val ticker: String
        val size: Int
        val recordBreakers = Stack<PriceDelta>()
        val queue: Queue<Double> = LinkedBlockingQueue()
        init {
            this.size   = size
            this.ticker = ticker
        }

        fun rank(price: Double) {
            if(queue.size < size) {
                queue.add(price)
                return
            }

            val oldPrice = queue.remove()
            queue.add(price)
            val newDelta = ((price - oldPrice)/oldPrice) * 100.0
            if(newDelta < 0) {
                return
            }

            if (recordBreakers.empty()) {
                //println("Adding new delta from empty recordBreakers -> NewDelta: $newDelta")
                recordBreakers.push(
                        PriceDelta(
                        value         = newDelta,
                        offset        = currentOffset
                    )
                )
                latestRankings.add(
                    DeltaRanking(
                        ticker            = ticker,
                        priceDelta        = newDelta,
                        price             = price,
                        rankChangePercent = 0.0,
                        timeWindow        = size,
                        distance          = currentOffset
                    )
                )
                return
            }
            var lastMax:PriceDelta = recordBreakers.peek()

            if(lastMax.isGreater(value = newDelta)) {
                //println("skipping spike -> Old value: ${lastMax.value}, NewDelta: $newDelta")
                return
            }

            while ((!recordBreakers.empty()) && (!recordBreakers.peek().isGreater(value = newDelta))) {
                lastMax = recordBreakers.pop()
            }
            //println("New delta ($newDelta), oldPrice: $oldPrice, newPrice: $price taking over window(${this.size}) at offset $newOffset from offset: $currentOffset")
            recordBreakers.push(
                lastMax.overWriteValue(value = newDelta)
            )
            latestRankings.add(
                DeltaRanking(
                    ticker            = ticker,
                    priceDelta        = newDelta,
                    price             = price,
                    rankChangePercent = lastMax.calculateChangePercent(newValue = newDelta),
                    timeWindow        = size,
                    distance          = lastMax.calculateDistance()
                )
            )
        }

        private class PriceDelta(value: Double, offset: Int) {
            private val value: Double
            private val offset: Int
            init {
                this.value         = value
                this.offset        = offset
            }

            fun isGreater(value: Double) : Boolean {
                return this.value > value
            }

            fun overWriteValue(value: Double) : PriceDelta {
                return PriceDelta(value = value, offset = offset)
            }

            fun calculateDistance() : Int {
                return currentOffset - this.offset
            }

            fun calculateChangePercent(newValue: Double) : Double {
                if(this.value == 0.0) {
                    return 100.0
                }
                return ((newValue - this.value) / this.value) * 100
            }
        }
    }

    class DeltaRanking(
        val ticker: String,
        val price: Double,
        val priceDelta: Double,
        val rankChangePercent: Double,
        val timeWindow: Int,
        val distance: Int
    ) {
        val propabilityCoefficient: Int
        init {
            this.propabilityCoefficient = this.timeWindow * this.distance
        }
    }
}