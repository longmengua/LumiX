package com.lumix.ethereum.finality;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lumix.ethereum.rpc.EthereumBlock;
import java.math.BigInteger;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EthereumFinalityServiceTest {
    private final EthereumFinalityService service = new EthereumFinalityService();

    @Test
    void finalizesOnlyAtOrBeforeFinalizedCheckpoint() {
        EthereumBlock deposit = block(100, "1");
        assertEquals(EthereumFinalityDecision.CONFIRMING, service.assess(deposit, deposit, block(99, "2")));
        assertEquals(EthereumFinalityDecision.FINALIZED, service.assess(deposit, deposit, block(100, "3")));
    }

    @Test
    void canonicalHashMismatchIsReorgAndNeverFinalized() {
        assertEquals(EthereumFinalityDecision.REORGED,
            service.assess(block(100, "1"), block(100, "2"), block(200, "3")));
    }

    private static EthereumBlock block(long height, String suffix) {
        return new EthereumBlock(BigInteger.valueOf(height), "0x" + "0".repeat(63) + suffix, Instant.EPOCH);
    }
}
