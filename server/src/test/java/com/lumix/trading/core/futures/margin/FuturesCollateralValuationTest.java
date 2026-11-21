package com.lumix.trading.core.futures.margin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 折算率須同時有政策版本、具名來源與有效期限，過期不得增加可開倉額度。 */
class FuturesCollateralValuationTest {
    private static final Instant OBSERVED = Instant.parse("2026-11-22T00:00:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-11-22T00:00:10Z");

    @Test
    void haircutAndRoundingNeverIncreaseCollateralValue() {
        var rate = new FuturesCollateralValuation.Rate("BTC", "named-index", 2,
                new BigDecimal("60000"), new BigDecimal("0.20"), OBSERVED, EXPIRES);
        assertEquals(new BigDecimal("4800.00"), FuturesCollateralValuation.valueInUsdt(
                new BigDecimal("0.1"), rate, OBSERVED.plusSeconds(1), 2));
    }

    @Test
    void staleOrInvalidRateFailsClosed() {
        var rate = new FuturesCollateralValuation.Rate("BTC", "named-index", 2,
                new BigDecimal("60000"), new BigDecimal("0.20"), OBSERVED, EXPIRES);
        assertThrows(IllegalArgumentException.class, () -> FuturesCollateralValuation.valueInUsdt(
                new BigDecimal("0.1"), rate, EXPIRES, 2));
        assertThrows(IllegalArgumentException.class, () -> FuturesCollateralValuation.valueInUsdt(
                new BigDecimal("-1"), rate, OBSERVED, 2));
        assertThrows(IllegalArgumentException.class, () -> new FuturesCollateralValuation.Rate("BTC", "",
                2, BigDecimal.ONE, BigDecimal.ZERO, OBSERVED, EXPIRES));
    }
}
