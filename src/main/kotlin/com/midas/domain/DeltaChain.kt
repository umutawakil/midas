package com.midas.domain

import com.midas.repositories.DeltaChainRepository
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import kotlin.collections.HashMap

@Entity
@Table(name="delta_chain")
class DeltaChain {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long               = -1L
    private val ticker       : String
    private val initialPrice : Double
    private var lowestDelta  : Double? = null
    private var highestDelta : Double? = null
    private var chainLength  : Int     = 0
    private var average      : Double? = null
    private var dead         : Boolean = false
    private val creationTime : Date

    @OneToMany(fetch = FetchType.EAGER, mappedBy="deltaChain", cascade = arrayOf(CascadeType.ALL))
    @OrderBy("chainPosition ASC")
    private val deltas: MutableList<Delta>

    constructor(date: Date, ticker: String, initialPrice: Double) {
        this.ticker       = ticker
        this.initialPrice = initialPrice
        this.deltas       = mutableListOf()
        this.creationTime = date
    }

    @Component
    class SpringAdapter(
        @Autowired private val deltaChainRepository: DeltaChainRepository,
        @Autowired private val loggingService: LoggingService
    ) {
        @PostConstruct
        fun init() {
            DeltaChain.deltaChainRepository = deltaChainRepository
            DeltaChain.springAdapter        = this
            val loadedChains: List<DeltaChain> = deltaChainRepository.findAll().toList()
            loggingService.log("Chains loaded: ${loadedChains.size}")
            for(d in deltaChainRepository.findAll()) {
                deltaChains[d.ticker] = d
            }
        }
    }
    companion object {
        val deltaChains: MutableMap<String, DeltaChain> = HashMap()
        private lateinit var deltaChainRepository: DeltaChainRepository
        private lateinit var springAdapter: SpringAdapter

        /** Only for integration testing **/
        fun init() {
            springAdapter.init()
        }

        fun addDeltas(date: Date, stocks: List<Pair<String, Double>>) {
            stocks.forEach {
                addDelta(date = date, ticker = it.first, price = it.second)
            }
        }
        private fun addDelta(date: Date, ticker: String, price: Double) {
            var deltaChain: DeltaChain? = deltaChains[ticker]
            if(deltaChain == null) {
                deltaChain = save (
                    DeltaChain(
                        date         = date,
                        ticker       = ticker,
                        initialPrice = price
                    )
                )
            } else {
                lateinit var newDelta: Delta
                if (deltaChain.chainLength == 0) {
                    val tempDelta = ((price - deltaChain.initialPrice) / deltaChain.initialPrice) *100.0
                    if(tempDelta <= 0.0) {
                        killChainAndStartNewChain(date = date, chain = deltaChain, lastPrice = price)
                        return
                    }
                    newDelta = Delta(
                        date       = date,
                        deltaChain = deltaChain,
                        priceDelta = tempDelta,
                        price      = price
                    )
                } else {
                    val tempDelta = ((price - deltaChain.deltas[deltaChain.deltas.size - 1].price)/deltaChain.deltas[deltaChain.deltas.size - 1].price)*100.0
                    if(tempDelta <= 0.0 ) {
                        killChainAndStartNewChain(date = date, chain = deltaChain, lastPrice = price)
                        return
                    }
                    newDelta = Delta(
                        date       = date,
                        deltaChain = deltaChain,
                        priceDelta = tempDelta,
                        price      = price
                    )
                }
                deltaChain.add(newDelta)
            }
            deltaChains[ticker] = save(deltaChain)
        }

        private fun save(chain: DeltaChain) : DeltaChain {
            return deltaChainRepository.save(chain)
        }
        private fun killChainAndStartNewChain(date: Date, chain: DeltaChain, lastPrice: Double) {
            chain.dead = true
            deltaChains.remove(chain.ticker)
            save(chain)

            /** Start a new chain **/
            addDelta(date = date, ticker = chain.ticker, price = lastPrice)
        }
    }

    private fun add(newDelta: Delta) {
        this.lowestDelta  = if ((this.lowestDelta  == null) || newDelta.priceDelta < this.lowestDelta!!)  newDelta.priceDelta else this.lowestDelta
        this.highestDelta = if ((this.highestDelta == null) || newDelta.priceDelta > this.highestDelta!!) newDelta.priceDelta else this.highestDelta
        if (this.average == null) {
            this.average = newDelta.priceDelta
        } else {
            this.average = (this.average!! + newDelta.priceDelta) / 2
        }
        newDelta.chainPosition = this.chainLength
        this.chainLength++
        this.deltas.add(newDelta)
    }

    @Entity(name="Delta")
    @Table(name="delta")
    private class Delta {
        @Id
        @Column(name="id")
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long = -1L
        val priceDelta: Double
        val price: Double
        var chainPosition: Int = 0
        val creationTime: Date

        @ManyToOne
        @JoinColumn(name="delta_chain_id", nullable=false)
        private val deltaChain: DeltaChain

        constructor(deltaChain: DeltaChain, date: Date, priceDelta: Double, price: Double) {
            this.deltaChain    = deltaChain
            this.priceDelta    = priceDelta
            this.price         = price
            this.creationTime  = date
        }
    }

    class Test {
        companion object {
            fun clear() {
                deltaChainRepository.deleteAll()
                deltaChains.clear()
            }
            fun clearMemoryOnly() {
                deltaChains.clear()
            }
            fun testCanSaveConsecutivePriceDeltas() {
                val ticker = "MDS"
                val date   = Date(System.currentTimeMillis())
                addDeltas(date = date, listOf(Pair(ticker,  5.0)))
                val deltaChain1 = deltaChains["MDS"]!!
                Assertions.assertTrue(deltaChain1.chainLength  == 0)
                Assertions.assertTrue(deltaChain1.average      == null)
                Assertions.assertTrue(deltaChain1.lowestDelta  == null)
                Assertions.assertTrue(deltaChain1.highestDelta == null)
                Assertions.assertTrue(deltaChain1.initialPrice == 5.0)

                addDeltas(date = date, listOf(Pair(ticker,10.0)))
                val deltaChain2 = deltaChains["MDS"]!!
                Assertions.assertTrue(deltaChain2.chainLength  == 1)
                Assertions.assertTrue(deltaChain2.average      == 100.0)
                Assertions.assertTrue(deltaChain2.lowestDelta  == 100.0)
                Assertions.assertTrue(deltaChain2.highestDelta == 100.0)
                Assertions.assertTrue(deltaChain2.initialPrice == 5.0)

                addDeltas(date = date, listOf(Pair(ticker, 30.0)))
                val deltaChain3 = deltaChains["MDS"]!!
                Assertions.assertEquals(2, deltaChain3.chainLength)
                Assertions.assertEquals(150.0,deltaChain3.average)
                Assertions.assertEquals(100.0, deltaChain3.lowestDelta)
                Assertions.assertEquals(200.0, deltaChain3.highestDelta)
                Assertions.assertEquals(5.0, deltaChain3.initialPrice)

                addDeltas(date = date, listOf(Pair(ticker, 31.5)))
                val deltaChain4 = deltaChains["MDS"]!!
                Assertions.assertEquals(3,   deltaChain4.chainLength)
                Assertions.assertEquals(77.5,  deltaChain4.average)
                Assertions.assertEquals(5.0,   deltaChain4.lowestDelta)
                Assertions.assertEquals(200.0, deltaChain4.highestDelta)
                Assertions.assertEquals(5.0,   deltaChain4.initialPrice)
            }

            fun testCanSaveConsecutivePriceDeltasAcrossTickers() {
                val ticker1 = "MDS"
                val ticker2 = "TST1"
                val date   = Date(System.currentTimeMillis())
                addDeltas(date = date, listOf(Pair(ticker1, 5.0)))
                addDeltas(date = date,listOf(Pair(ticker2, 100.0)))

                /** Check first ticker is okay **/
                val deltaChain1 = deltaChains[ticker1]!!
                Assertions.assertEquals(0, deltaChain1.chainLength)
                Assertions.assertEquals(null, deltaChain1.average)
                Assertions.assertEquals(null, deltaChain1.lowestDelta)
                Assertions.assertEquals(null, deltaChain1.highestDelta)
                Assertions.assertEquals(5.0,  deltaChain1.initialPrice)

                /** Check second ticker is okay **/
                val deltaChainB = deltaChains[ticker2]!!
                Assertions.assertEquals(0, deltaChainB.chainLength)
                Assertions.assertEquals(null, deltaChainB.average)
                Assertions.assertEquals(null, deltaChainB.lowestDelta)
                Assertions.assertEquals(null, deltaChainB.highestDelta)
                Assertions.assertEquals(100.0,deltaChainB.initialPrice )

                addDeltas(date = date, listOf(Pair(ticker1, 10.0)))
                addDeltas(date = date, listOf(Pair(ticker2, 300.0)))

                /** Check 1st ticker is okay again **/
                val deltaChain2 = deltaChains[ticker1]!!
                Assertions.assertEquals(1, deltaChain2.chainLength)
                Assertions.assertEquals(100.0, deltaChain2.average)
                Assertions.assertEquals(100.0, deltaChain2.lowestDelta)
                Assertions.assertEquals(100.0, deltaChain2.highestDelta)
                Assertions.assertEquals(5.0, deltaChain2.initialPrice)

                /** Check the other tickers info is correct **/
                val deltaChain3 = deltaChains[ticker2]!!
                Assertions.assertEquals(1, deltaChain3.chainLength)
                Assertions.assertEquals(200.0, deltaChain3.average)
                Assertions.assertEquals(200.0, deltaChain3.lowestDelta)
                Assertions.assertEquals(200.0, deltaChain3.highestDelta)
                Assertions.assertEquals(100.0, deltaChain3.initialPrice)
            }

            fun testWillRejectNegativeDeltas() {
                val ticker1 = "MDS"
                val ticker2 = "TST1"
                val date   = Date(System.currentTimeMillis())
                addDeltas(date = date, listOf(Pair(ticker1, 5.0)))
                addDeltas(date = date, listOf(Pair(ticker2, 100.0)))

                /** Check first ticker is okay **/
                val deltaChain1 = deltaChains[ticker1]!!
                Assertions.assertEquals(0, deltaChain1.chainLength)
                Assertions.assertEquals(null, deltaChain1.average)
                Assertions.assertEquals(null, deltaChain1.lowestDelta)
                Assertions.assertEquals(null, deltaChain1.highestDelta)
                Assertions.assertEquals(5.0,  deltaChain1.initialPrice)

                /** Check second ticker is okay **/
                val deltaChainB = deltaChains[ticker2]!!
                Assertions.assertEquals(0, deltaChainB.chainLength)
                Assertions.assertEquals(null, deltaChainB.average)
                Assertions.assertEquals(null, deltaChainB.lowestDelta)
                Assertions.assertEquals(null, deltaChainB.highestDelta)
                Assertions.assertEquals(100.0,deltaChainB.initialPrice )

                addDeltas(date = date, listOf(Pair(ticker1, 10.0)))
                addDeltas(date = date, listOf(Pair(ticker2, 50.0)))

                /** Check 1st ticker is okay again **/
                val deltaChain2 = deltaChains[ticker1]!!
                Assertions.assertEquals(1, deltaChain2.chainLength)
                Assertions.assertEquals(100.0, deltaChain2.average)
                Assertions.assertEquals(100.0, deltaChain2.lowestDelta)
                Assertions.assertEquals(100.0, deltaChain2.highestDelta)
                Assertions.assertEquals(5.0, deltaChain2.initialPrice)

                /** Check the other tickers info is replaced with a new chain since the old should
                 * be killed since it terminated on the negative **/
                val deltaChain3 = deltaChains[ticker2]!!
                Assertions.assertEquals(0, deltaChain3.chainLength)
                Assertions.assertEquals(null, deltaChain3.average)
                Assertions.assertEquals(null, deltaChain3.lowestDelta)
                Assertions.assertEquals(null, deltaChain3.highestDelta)
                Assertions.assertEquals(50.0,deltaChain3.initialPrice ) //A new chain with the last value at the head
            }

            fun testCanReloadFromDisk() {
                DeltaChain.Test.clearMemoryOnly()
                val ticker1 = "MDS"
                val ticker2 = "TST1"
                val date   = Date(System.currentTimeMillis())
                addDeltas(date = date, listOf(Pair(ticker1, 5.0)))
                addDeltas(date = date, listOf(Pair(ticker2, 100.0)))

                DeltaChain.Test.clearMemoryOnly()
                Assertions.assertEquals(null, deltaChains[ticker1])
                Assertions.assertEquals(null, deltaChains[ticker2])
                DeltaChain.init()

                /** Check first ticker is okay **/
                val deltaChain1 = deltaChains[ticker1]!!
                Assertions.assertEquals(0, deltaChain1.chainLength)
                Assertions.assertEquals(null, deltaChain1.average)
                Assertions.assertEquals(null, deltaChain1.lowestDelta)
                Assertions.assertEquals(null, deltaChain1.highestDelta)
                Assertions.assertEquals(5.0,  deltaChain1.initialPrice)

                /** Check second ticker is okay **/
                val deltaChainB = deltaChains[ticker2]!!
                Assertions.assertEquals(0, deltaChainB.chainLength)
                Assertions.assertEquals(null, deltaChainB.average)
                Assertions.assertEquals(null, deltaChainB.lowestDelta)
                Assertions.assertEquals(null, deltaChainB.highestDelta)
                Assertions.assertEquals(100.0,deltaChainB.initialPrice )
            }

            fun testWillRejectZeroDeltas() {
                val ticker1 = "MDS"
                val ticker2 = "TST1"
                val date   = Date(System.currentTimeMillis())
                addDeltas(date = date, listOf(Pair(ticker1, 5.0)))
                addDeltas(date = date, listOf(Pair(ticker2, 100.0)))

                /** Check first ticker is okay **/
                val deltaChain1 = deltaChains[ticker1]!!
                Assertions.assertEquals(0, deltaChain1.chainLength)
                Assertions.assertEquals(null, deltaChain1.average)
                Assertions.assertEquals(null, deltaChain1.lowestDelta)
                Assertions.assertEquals(null, deltaChain1.highestDelta)
                Assertions.assertEquals(5.0,  deltaChain1.initialPrice)

                /** Check second ticker is okay **/
                val deltaChainB = deltaChains[ticker2]!!
                Assertions.assertEquals(0, deltaChainB.chainLength)
                Assertions.assertEquals(null, deltaChainB.average)
                Assertions.assertEquals(null, deltaChainB.lowestDelta)
                Assertions.assertEquals(null, deltaChainB.highestDelta)
                Assertions.assertEquals(100.0,deltaChainB.initialPrice )

                addDeltas(date = date, listOf(Pair(ticker1, 10.0)))
                addDeltas(date = date, listOf(Pair(ticker2, 100.0)))

                /** Check 1st ticker is okay again **/
                val deltaChain2 = deltaChains[ticker1]!!
                Assertions.assertEquals(1, deltaChain2.chainLength)
                Assertions.assertEquals(100.0, deltaChain2.average)
                Assertions.assertEquals(100.0, deltaChain2.lowestDelta)
                Assertions.assertEquals(100.0, deltaChain2.highestDelta)
                Assertions.assertEquals(5.0, deltaChain2.initialPrice)

                /** Check the other tickers info is replaced with a new chain since the old should
                 * be killed since it terminated on the negative **/
                val deltaChain3 = deltaChains[ticker2]!!
                Assertions.assertEquals(0, deltaChain3.chainLength)
                Assertions.assertEquals(null, deltaChain3.average)
                Assertions.assertEquals(null, deltaChain3.lowestDelta)
                Assertions.assertEquals(null, deltaChain3.highestDelta)
                Assertions.assertEquals(100.0,deltaChain3.initialPrice ) //A new chain with the last value at the head
            }
        }
    }
}