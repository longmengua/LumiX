package com.lumix.marketfeed.binance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 來源時間、商品身分與 decimal 價格都須先驗證，過期行情不可成為風險決策輸入。 */
class BinanceFuturesReferencePriceFeedTest {
    private static final Instant RECEIVED = Instant.ofEpochMilli(1700000001000L);
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void parsesNamedMarkAndIndexWithoutBinaryFloatingPoint() throws Exception {
        var snapshot = BinanceFuturesReferencePriceFeed.parse(json.readTree(payload("BTCUSDT", "60000.12345678", "59990.12345678", 1700000000000L)),
                "BTCUSDT", RECEIVED);
        assertEquals(new BigDecimal("60000.12345678"), snapshot.markPrice());
        assertEquals(new BigDecimal("59990.12345678"), snapshot.indexPrice());
        assertTrue(BinanceFuturesReferencePriceFeed.fresh(snapshot, Duration.ofSeconds(2), RECEIVED).isPresent());
        assertTrue(BinanceFuturesReferencePriceFeed.fresh(snapshot, Duration.ofSeconds(2), RECEIVED.plusSeconds(2)).isEmpty());
    }

    @Test
    void rejectsWrongSymbolInvalidPriceAndFutureTimestamp() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> BinanceFuturesReferencePriceFeed.parse(
                json.readTree(payload("ETHUSDT", "60000", "59990", 1700000000000L)), "BTCUSDT", RECEIVED));
        assertThrows(IllegalArgumentException.class, () -> BinanceFuturesReferencePriceFeed.parse(
                json.readTree(payload("BTCUSDT", "1e20", "59990", 1700000000000L)), "BTCUSDT", RECEIVED));
        assertThrows(IllegalArgumentException.class, () -> BinanceFuturesReferencePriceFeed.parse(
                json.readTree(payload("BTCUSDT", "60000", "59990", 1700000002000L)), "BTCUSDT", RECEIVED));
    }

    private static String payload(String symbol, String mark, String index, long time) {
        return "{\"symbol\":\"" + symbol + "\",\"markPrice\":\"" + mark
                + "\",\"indexPrice\":\"" + index + "\",\"time\":" + time + "}";
    }
}
