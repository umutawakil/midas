package com.midas.integrationTests

import com.midas.domain.DeltaChain
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DeltaChainIntegrationTest {
    @BeforeEach
    fun init() {
       /**Clear() **/
        DeltaChain.Test.clear()
    }

    @Order(0)
    @Test
    fun Can_save_consecutive_increasing_price_deltas_of_a_sequence() {
        DeltaChain.Test.testCanSaveConsecutivePriceDeltas()
    }

    @Order(1)
    @Test
    fun Can_save_consecutive_price_deltas_across_multiple_tickers() {
        DeltaChain.Test.testCanSaveConsecutivePriceDeltasAcrossTickers()
    }

    @Order(2)
    @Test
    fun will_reject_negative_deltas() {
        DeltaChain.Test.testWillRejectNegativeDeltas()
    }

    @Order(3)
    @Test
    fun Can_reload_from_disk() {
        DeltaChain.Test.testCanReloadFromDisk()
    }
}