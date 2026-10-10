package com.lumix.ethereum.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 驗證 scanner 僅產生可追溯的 observation candidate，沒有任何 ledger 或餘額效果。 */
class EthereumDepositScannerTest {
    private static final String HASH = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String BLOCK_HASH = "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String FROM = "0x1111111111111111111111111111111111111111";
    private static final String TO = "0x2222222222222222222222222222222222222222";

    @Test
    void detectsOnlyPositiveDirectEthToManagedAddress() {
        EthereumEthDepositScanner scanner = new EthereumEthDepositScanner();
        EthereumDirectTransfer transfer = new EthereumDirectTransfer(HASH, FROM, Optional.of(TO), BigInteger.ONE, BigInteger.TEN, BLOCK_HASH);

        EthereumDepositCandidate candidate = scanner.detect(transfer, Map.of(TO, "account-1")).orElseThrow();

        assertEquals("ETH", candidate.assetCode());
        assertEquals(BigInteger.ONE, candidate.amountRaw());
        assertTrue(scanner.detect(transfer, Map.of()).isEmpty());
    }

    @Test
    void parsesOfficialUsdtTransferAndKeepsLogIndexIdentity() {
        EthereumUsdtTransferLog transfer = EthereumUsdtTransferLog.fromRaw(
                "0xdAC17F958D2ee523a2206206994597C13D831ec7", HASH, 7L, BigInteger.TEN, BLOCK_HASH,
                List.of(EthereumUsdtTransferLog.TRANSFER_TOPIC, word(FROM), word(TO)), word("0x00000000000000000000000000000000000000000000000000000000000f4240"));

        EthereumDepositCandidate candidate = new EthereumUsdtDepositScanner().detect(transfer, Map.of(TO, "account-1")).orElseThrow();

        assertEquals("USDT", candidate.assetCode());
        assertEquals(7L, candidate.logIndex());
        assertEquals(new BigInteger("1000000"), candidate.amountRaw());
    }

    private static String word(String addressOrWord) {
        String hex = addressOrWord.substring(2);
        return "0x" + "0".repeat(64 - hex.length()) + hex;
    }
}
