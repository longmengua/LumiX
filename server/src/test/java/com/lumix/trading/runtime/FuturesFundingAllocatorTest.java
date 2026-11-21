package com.lumix.trading.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.junit.jupiter.api.Test;

/** funding 必須 USDT 零和、明確取整，且不能吃穿逐倉維持保證金。 */
class FuturesFundingAllocatorTest {
    @Test
    void positiveAndNegativeRatesReversePayerWithoutCreatingUsdt() {
        var positions = List.of(new FuturesFundingAllocator.Position("alice", "LONG", d("0.1"), d("5")),
                new FuturesFundingAllocator.Position("bob", "SHORT", d("0.1"), d("5")));
        var positive = FuturesFundingAllocator.allocate(positions, d("100"), d("0.01"),
                d("0.05"), 6, RoundingMode.DOWN);
        assertEquals(d("-0.100000"), positive.get(0).fundingUsdt());
        assertEquals(d("0.100000"), positive.get(1).fundingUsdt());
        assertEquals(d("4.900000"), positive.get(0).resultingCollateralUsdt());
        var negative = FuturesFundingAllocator.allocate(positions, d("100"), d("-0.01"),
                d("0.05"), 6, RoundingMode.DOWN);
        assertEquals(d("0.100000"), negative.get(0).fundingUsdt());
        assertEquals(d("-0.100000"), negative.get(1).fundingUsdt());
    }

    @Test
    void unmatchedOpenInterestAndUnderfundedPayerAreRejected() {
        assertThrows(TradingOrderRejectedException.class, () -> FuturesFundingAllocator.allocate(
                List.of(new FuturesFundingAllocator.Position("alice", "LONG", d("0.1"), d("5"))),
                d("100"), d("0.01"), d("0.05"), 6, RoundingMode.DOWN));
        assertThrows(TradingOrderRejectedException.class, () -> FuturesFundingAllocator.allocate(
                List.of(new FuturesFundingAllocator.Position("alice", "LONG", d("0.1"), d("0.5")),
                        new FuturesFundingAllocator.Position("bob", "SHORT", d("0.1"), d("5"))),
                d("100"), d("0.01"), d("0.05"), 6, RoundingMode.DOWN));
    }

    private static BigDecimal d(String value) { return new BigDecimal(value); }
}
