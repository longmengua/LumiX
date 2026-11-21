package com.lumix.trading.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.marketfeed.binance.BinanceFuturesReferencePriceFeed;
import com.lumix.trading.core.futures.margin.FuturesCollateralValuation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 覆蓋標記價與委託價、行情過期、槓桿及非 USDT 兌付額度。 */
class FuturesOrderRiskGateTest {
    private static final Instant NOW = Instant.parse("2026-11-22T00:00:00Z");
    private static final String SOURCE = "BINANCE_USDSM_PREMIUM_INDEX";

    @Test
    void usdtMarginUsesWorstPriceAndRequestedLeverage() {
        var decision = FuturesOrderRiskGate.admit(policy("USDT"), reference(NOW), null,
                decimal("0.1"), decimal("101"), decimal("5"), BigDecimal.ZERO,
                decimal("3"), BigDecimal.ZERO, BigDecimal.ZERO, NOW);
        assertEquals(decimal("10.100000"), decision.orderNotionalUsdt());
        assertEquals(decimal("2.020000"), decision.initialMarginUsdt());
        assertEquals(decimal("0.020200"), decision.maximumFeeUsdt());
        assertEquals(0, decision.collateralRatePolicyVersion());
        assertThrows(TradingOrderRejectedException.class, () -> FuturesOrderRiskGate.admit(
                policy("USDT"), reference(NOW), null, decimal("0.1"), decimal("101"),
                decimal("6"), BigDecimal.ZERO, decimal("3"), BigDecimal.ZERO, BigDecimal.ZERO, NOW));
    }

    @Test
    void staleReferenceAndInsufficientCollateralFailClosed() {
        assertThrows(TradingOrderRejectedException.class, () -> FuturesOrderRiskGate.admit(
                policy("USDT"), reference(NOW.minusSeconds(11)), null,
                decimal("0.1"), decimal("90"), decimal("5"), BigDecimal.ZERO,
                decimal("3"), BigDecimal.ZERO, BigDecimal.ZERO, NOW));
        assertThrows(TradingOrderRejectedException.class, () -> FuturesOrderRiskGate.admit(
                policy("USDT"), reference(NOW), null,
                decimal("0.1"), decimal("90"), decimal("5"), BigDecimal.ZERO,
                decimal("2"), BigDecimal.ZERO, BigDecimal.ZERO, NOW));
    }

    @Test
    void nonUsdtMarginRequiresFreshRateAndFundedBoundedPool() {
        var rate = new FuturesCollateralValuation.Rate("BTC", "BINANCE_SPOT", 7,
                decimal("100000"), decimal("0.2"), NOW.minusSeconds(1), NOW.plusSeconds(9));
        var accepted = FuturesOrderRiskGate.admit(policy("BTC"), reference(NOW), rate,
                decimal("0.1"), decimal("101"), decimal("5"), BigDecimal.ZERO,
                decimal("0.00004"), BigDecimal.ZERO, decimal("3"), NOW);
        assertEquals(decimal("3.200000"), accepted.collateralValueUsdt());
        assertEquals(7, accepted.collateralRatePolicyVersion());
        assertThrows(TradingOrderRejectedException.class, () -> FuturesOrderRiskGate.admit(
                policy("BTC"), reference(NOW), rate, decimal("0.1"), decimal("101"),
                decimal("5"), BigDecimal.ZERO, decimal("0.00004"), BigDecimal.ZERO,
                decimal("2"), NOW));
        assertThrows(TradingOrderRejectedException.class, () -> FuturesOrderRiskGate.admit(
                policy("BTC"), reference(NOW), rate, decimal("0.1"), decimal("101"),
                decimal("5"), BigDecimal.ZERO, decimal("0.00004"), decimal("99999"),
                decimal("3"), NOW));
        assertThrows(IllegalArgumentException.class, () -> FuturesOrderRiskGate.admit(
                policy("BTC"), reference(NOW), rate, decimal("0.1"), decimal("101"),
                decimal("5"), BigDecimal.ZERO, decimal("0.00004"), BigDecimal.ZERO,
                decimal("3"), NOW.plusSeconds(10)));
    }

    private static BinanceFuturesReferencePriceFeed.Snapshot reference(Instant observed) {
        return new BinanceFuturesReferencePriceFeed.Snapshot(SOURCE, "BTCUSDT", decimal("100"),
                decimal("100"), observed, observed);
    }

    private static FuturesRiskPolicy policy(String collateral) {
        var rules = new TradingInstrumentRules("PERPETUAL:BTCUSDT", TradingInstrumentRules.Kind.PERPETUAL,
                "BTC-USDT", "BTC", "USDT", 1, 2, 4, 6, decimal("0.01"), decimal("0.0001"),
                decimal("0.0001"), decimal("100"), BigDecimal.ONE, decimal("10000000"),
                decimal("0.001"), decimal("0.002"), RoundingMode.DOWN);
        var execution = new TradingExecutionPolicy(rules, "fees", 100, decimal("0.05"),
                TradingExecutionPolicy.FeeAccrual.CUMULATIVE_QUOTE,
                TradingExecutionPolicy.CounterpartyModel.CLIENT_TO_CLIENT);
        return new FuturesRiskPolicy(execution, collateral,
                "USDT".equals(collateral) ? BigDecimal.ZERO : decimal("0.2"), decimal("5"),
                decimal("0.05"), decimal("1000000"), SOURCE, SOURCE, Duration.ofSeconds(10),
                Instant.EPOCH, Duration.ofHours(8), decimal("0.01"), decimal("100000"), RoundingMode.DOWN,
                "margin", "insurance", "pool");
    }

    private static BigDecimal decimal(String value) { return new BigDecimal(value); }
}
