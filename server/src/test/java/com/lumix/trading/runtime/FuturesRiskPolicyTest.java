package com.lumix.trading.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** 政策 fixture 只在測試中使用，驗證雙向 USDT 永續所需來源與資金責任不可省略。 */
class FuturesRiskPolicyTest {
    @Test
    void explicitRiskPolicyCarriesCollateralAndFundingBoundaries() {
        var policy = policy("BTC", "insurance-usdt", "conversion-usdt");
        assertEquals("BTC", policy.collateralAsset());
        assertEquals("named-futures-mark", policy.markPriceSource());
        assertEquals("conversion-usdt", policy.conversionPoolAccountId());
    }

    @Test
    void missingOrSharedFundingAccountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> policy("BTC", "same", "same"));
        assertThrows(IllegalArgumentException.class, () -> policy("", "insurance-usdt", "conversion-usdt"));
        var valid = policy("BTC", "insurance-usdt", "conversion-usdt");
        assertThrows(IllegalArgumentException.class, () -> new FuturesRiskPolicy(valid.execution(), "BTC",
                decimal("0.2"), decimal("5"), decimal("0.2"), decimal("1000000"),
                "named-futures-mark", "named-futures-index", Duration.ofSeconds(10),
                Duration.ofHours(8), decimal("0.01"), "insurance-usdt", "conversion-usdt"));
    }

    private static FuturesRiskPolicy policy(String collateral, String insurance, String conversion) {
        var rules = new TradingInstrumentRules("PERPETUAL:BTCUSDT", TradingInstrumentRules.Kind.PERPETUAL,
                "BTC-USDT", "BTC", "USDT", 1, 2, 4, 6,
                decimal("0.01"), decimal("0.0001"), decimal("0.0001"), decimal("100"),
                BigDecimal.ONE, decimal("10000000"), decimal("0.001"), decimal("0.002"), RoundingMode.DOWN);
        var execution = new TradingExecutionPolicy(rules, "fees", 100, decimal("0.05"),
                TradingExecutionPolicy.FeeAccrual.CUMULATIVE_QUOTE,
                TradingExecutionPolicy.CounterpartyModel.CLIENT_TO_CLIENT);
        return new FuturesRiskPolicy(execution, collateral, decimal("0.2"), decimal("5"),
                decimal("0.05"), decimal("1000000"), "named-futures-mark", "named-futures-index",
                Duration.ofSeconds(10), Duration.ofHours(8), decimal("0.01"), insurance, conversion);
    }

    private static BigDecimal decimal(String value) { return new BigDecimal(value); }
}
