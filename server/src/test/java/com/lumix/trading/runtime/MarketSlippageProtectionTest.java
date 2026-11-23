package com.lumix.trading.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MarketSlippageProtectionTest {
    private static final BigDecimal BEST = new BigDecimal("5");
    private static final BigDecimal TICK = new BigDecimal("0.01");
    private static final BigDecimal MAXIMUM = new BigDecimal("0.1");

    @Test void percentageAndFixedBoundsNeverRoundBeyondCustomerLimit() {
        assertEquals(new BigDecimal("5.02"), MarketSlippageProtection.bound(BEST, "BUY", TICK,
                MAXIMUM, "PERCENT", "0.5"));
        assertEquals(new BigDecimal("4.98"), MarketSlippageProtection.bound(BEST, "SELL", TICK,
                MAXIMUM, "PERCENT", "0.5"));
        assertEquals(new BigDecimal("5.03"), MarketSlippageProtection.bound(BEST, "BUY", TICK,
                MAXIMUM, "FIXED_USDT", "0.039"));
        assertEquals(new BigDecimal("4.97"), MarketSlippageProtection.bound(BEST, "SELL", TICK,
                MAXIMUM, "FIXED_USDT", "0.039"));
    }

    @Test void rejectsToleranceBeyondGovernedLimitAndIncompleteInputs() {
        assertThrows(TradingOrderRejectedException.class, () -> MarketSlippageProtection.bound(BEST,
                "BUY", TICK, MAXIMUM, "PERCENT", "11"));
        assertThrows(TradingOrderRejectedException.class, () -> MarketSlippageProtection.bound(BEST,
                "SELL", TICK, MAXIMUM, "FIXED_USDT", "0.51"));
        assertThrows(IllegalArgumentException.class, () -> MarketSlippageProtection.bound(BEST,
                "BUY", TICK, MAXIMUM, "PERCENT", null));
    }
}
