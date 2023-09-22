package com.midas.domain

import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class DeltaRanker {
    companion object {
        //TODO: ALl the core object classes need to be loaded from the database so the system can pick up
        //  where it left off
        // Needs a comparator for decreasing and magnitude

        private val latestRankings = mutableListOf<PriceChangeMilestone>()
        private val tickerGroups   = mutableMapOf<String, TickerGroup>()

        //TODO: Needs to be initialized from the database or size of StockPriceInfo
        private var currentOffset = 0L

        fun rank(stocks: List<Pair<String, Double>>): List<PriceChangeMilestone> {
            latestRankings.clear()
            stocks.forEach {
                rank(
                    ticker = it.first,
                    price  = it.second
                )
            }

            currentOffset++

            return latestRankings
        }

        private fun rank(ticker: String, price: Double) {
            if(price <=0) {
                return
            }

            //TODO: if no entry exists for ticker then load a queue for each ticker with each queue representing a window

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
            PriceChangeMilestone.clear()
            currentOffset = 0
        }
    }

    private class TickerGroup {
        private val timeWindowEntries: MutableList<TimeWindowEntry> = mutableListOf()
        constructor(ticker: String) {
            initWindowGroups(ticker = ticker)
        }

        fun initWindowGroups(ticker: String) {
            timeWindowEntries.add(
                TimeWindowEntry(ticker = ticker, size = 1)
            )
            for (i in 1 until 13) {
                timeWindowEntries.add(
                    TimeWindowEntry(ticker = ticker, size = 5 * i)
                )
            }
        }

        fun rank(price: Double) {
            timeWindowEntries.forEach{ it.rank(price = price)}
        }
    }
    private class TimeWindowEntry(ticker: String, size: Int) {
        val ticker: String
        val size: Int

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

            PriceChangeMilestone.updateIfNewMilestone(
                ticker     = ticker,
                timeWindow = size,
                newDelta   = newDelta,
                price      = price
            )
        }


    }
    class PriceChangeMilestone(
        val ticker: String,
        val price: Double,
        val priceDelta: Double,
        val milestoneChange: Double,
        val timeWindow: Int,
        val distance: Long,
        val offset: Long
    ) {
        val probabilityCoefficient: Long
        init {
            this.probabilityCoefficient = this.timeWindow * this.distance
        }

        companion object {
            private val recordBreakers = Stack<PriceChangeMilestone>()

            fun clear() {
                this.recordBreakers.clear()
            }

            fun updateIfNewMilestone(ticker: String, timeWindow: Int, newDelta: Double, price: Double) {
                if (recordBreakers.empty()) {
                    //println("Adding new delta from empty recordBreakers -> NewDelta: $newDelta")
                    val newMilestone = PriceChangeMilestone(
                        ticker            = ticker,
                        priceDelta        = newDelta,
                        price             = price,
                        milestoneChange   = 0.0,
                        timeWindow        = timeWindow,
                        distance          = currentOffset,
                        offset            = currentOffset
                    )
                    recordBreakers.push(newMilestone)
                    latestRankings.add(newMilestone)
                    return
                }

                var lastMax:PriceChangeMilestone = recordBreakers.peek()
                if(lastMax.priceDelta > newDelta) {
                    //println("skipping spike -> Old value: ${lastMax.value}, NewDelta: $newDelta")
                    return
                }

                while ((!recordBreakers.empty()) && (recordBreakers.peek().priceDelta < newDelta)) {
                    lastMax = recordBreakers.pop()
                }
                //println("New delta ($newDelta), oldPrice: $oldPrice, newPrice: $price taking over window(${this.size}) at offset $newOffset from offset: $currentOffset")
                val updatedMilestone = PriceChangeMilestone(
                    ticker            = ticker,
                    priceDelta        = newDelta,
                    price             = price,
                    milestoneChange   = calculateMilestoneChange(newMilestone = newDelta, oldMilestone = lastMax.priceDelta),
                    timeWindow        = timeWindow,
                    distance          = currentOffset - lastMax.offset,
                    offset            = lastMax.offset //TODO: Does this produce the right results if you use a bad offset in tests?
                )
                recordBreakers.push(updatedMilestone)
                latestRankings.add(updatedMilestone)
            }
            private fun calculateMilestoneChange(newMilestone: Double, oldMilestone: Double) : Double {
                if(oldMilestone == 0.0) {
                    return 0.0 // was 100 but thats going to give false positives
                }
                return ((newMilestone - oldMilestone) / oldMilestone) * 100
            }
        }
    }
}