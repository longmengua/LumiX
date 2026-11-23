package com.lumix.trading.liquidity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.trading.runtime.TradingInstrumentRules;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 外部報價計算只驗證展示價格與深度上限，不假裝已在來源平台保留或成交。 */
class ExternalQuoteCalculatorTest {
    private static final Instant NOW = Instant.parse("2026-11-22T00:00:00Z");

    /** 來源賣價 5 USDT 加 0.2% 為 5.01；買價向下取 tick，兩側總量各受上層額度限制。 */
    @Test
    void baseAndAssetRateAdjustQuotesOutwardWithoutOverstatingQuantity() {
        var book = ExternalQuoteCalculator.quote(snapshot(List.of(level("4.90", "2")),
                List.of(level("5.00", "2"))), rules(), policy(), d("1.5"), NOW, Duration.ofSeconds(2)).orElseThrow();
        assertEquals(11, book.policyVersion());
        assertEquals(d("5.01"), book.asks().getFirst().price());
        assertEquals(d("4.89"), book.bids().getFirst().price());
        assertEquals(d("1.5"), book.asks().getFirst().quantity());
        assertEquals(d("1.5"), book.bids().getFirst().quantity());
    }

    /** 來源或接收時效失效必須撤掉整個報價，不能延用最後可見委託。 */
    @Test
    void staleAndFutureSnapshotsDoNotProduceQuotes() {
        var stale = new ExternalLiquiditySource.Snapshot("VENUE", TradingInstrumentRules.Kind.SPOT,
                "BTC-USDT", NOW.minusSeconds(3), NOW.minusSeconds(1), List.of(), List.of());
        assertTrue(ExternalQuoteCalculator.quote(stale, rules(), policy(), d("1"), NOW,
                Duration.ofSeconds(2)).isEmpty());
        var future = new ExternalLiquiditySource.Snapshot("VENUE", TradingInstrumentRules.Kind.SPOT,
                "BTC-USDT", NOW.plusSeconds(1), NOW.plusSeconds(1), List.of(), List.of());
        assertTrue(ExternalQuoteCalculator.quote(future, rules(), policy(), d("1"), NOW,
                Duration.ofSeconds(2)).isEmpty());
    }

    /** 來源倒序、交叉簿與商品不一致均拒絕，避免用錯商品或過時深度計價。 */
    @Test
    void malformedOrMismatchedBooksFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> ExternalQuoteCalculator.quote(
                snapshot(List.of(level("5.01", "1")), List.of(level("5.00", "1"))),
                rules(), policy(), d("1"), NOW, Duration.ofSeconds(2)));
        assertThrows(IllegalArgumentException.class, () -> ExternalQuoteCalculator.quote(
                snapshot(List.of(level("4.90", "1"), level("4.95", "1")), List.of()),
                rules(), policy(), d("1"), NOW, Duration.ofSeconds(2)));
        var wrongKind = new ExternalLiquiditySource.Snapshot("VENUE", TradingInstrumentRules.Kind.PERPETUAL,
                "BTC-USDT", NOW, NOW, List.of(), List.of());
        assertThrows(IllegalArgumentException.class, () -> ExternalQuoteCalculator.quote(
                wrongKind, rules(), policy(), d("1"), NOW, Duration.ofSeconds(2)));
    }

    /** 幣別折減可抵減全站費率，但最終費率不能為負或達到 100%。 */
    @Test
    void invalidEffectiveRateIsRejected() {
        assertEquals(d("0.001"), new ExternalQuotePolicy(1, "BTC", d("0.002"), d("0"), d("-0.001"), d("0")).effectiveRate());
        assertThrows(IllegalArgumentException.class,
                () -> new ExternalQuotePolicy(1, "BTC", d("0.001"), d("0"), d("-0.002"), d("0")));
        assertThrows(IllegalArgumentException.class,
                () -> new ExternalQuotePolicy(1, "BTC", d("0.002"), d("0"), d("0.999"), d("0")));
        assertThrows(IllegalArgumentException.class,
                () -> new ExternalQuotePolicy(1, "BTC", d("0"), d("0.1"), d("0"), d("-0.2")));
    }

    /** 同一固定 0.1 USDT 調整使來源買價 5、4 分別顯示 4.9、3.9，不能誤當百分比。 */
    @Test
    void fixedUsdtAdjustmentIsPerBaseUnit() {
        var book = ExternalQuoteCalculator.quote(snapshot(
                List.of(level("5", "2"), level("4", "1")), List.of(level("6", "2"))),
                rules(), new ExternalQuotePolicy(12, "BTC", d("0"), d("0.1"), d("0"), d("0")),
                d("10"), NOW, Duration.ofSeconds(2)).orElseThrow();
        assertEquals(d("4.90"), book.bids().get(0).price());
        assertEquals(d("3.90"), book.bids().get(1).price());
        assertEquals(d("6.10"), book.asks().getFirst().price());
    }

    private static ExternalLiquiditySource.Level level(String price, String quantity) {
        return new ExternalLiquiditySource.Level(d(price), d(quantity));
    }

    private static ExternalLiquiditySource.Snapshot snapshot(List<ExternalLiquiditySource.Level> bids,
                                                              List<ExternalLiquiditySource.Level> asks) {
        return new ExternalLiquiditySource.Snapshot("VENUE", TradingInstrumentRules.Kind.SPOT,
                "BTC-USDT", NOW, NOW, bids, asks);
    }

    private static ExternalQuotePolicy policy() {
        return new ExternalQuotePolicy(11, "BTC", d("0.001"), d("0"), d("0.001"), d("0"));
    }

    private static TradingInstrumentRules rules() {
        // 測試用商品數字不得移入正式政策或啟用配置。
        return new TradingInstrumentRules("BTC-USDT:SPOT", TradingInstrumentRules.Kind.SPOT,
                "BTC-USDT", "BTC", "USDT", 7, 2, 1, 3,
                d("0.01"), d("0.1"), d("0.1"), d("100"), d("1"), d("100000"),
                d("0.001"), d("0.001"), RoundingMode.CEILING);
    }

    private static BigDecimal d(String text) { return new BigDecimal(text); }
}
