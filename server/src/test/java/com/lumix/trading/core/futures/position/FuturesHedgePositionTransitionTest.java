package com.lumix.trading.core.futures.position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

/** 雙向持倉的成交方向必須由明確 position side 決定，避免反向成交意外開出另一邊倉位。 */
class FuturesHedgePositionTransitionTest {
    @Test
    void longAndShortCanCoexistAndSettleOppositePnl() {
        var longOpen = FuturesHedgePositionTransition.apply(null, FuturesPositionSide.LONG,
                FuturesHedgePositionTransition.OrderSide.BUY, value("2"), value("100"), false, 18, RoundingMode.HALF_EVEN);
        var shortOpen = FuturesHedgePositionTransition.apply(null, FuturesPositionSide.SHORT,
                FuturesHedgePositionTransition.OrderSide.SELL, value("2"), value("100"), false, 18, RoundingMode.HALF_EVEN);

        var longClose = FuturesHedgePositionTransition.apply(longOpen.position(), FuturesPositionSide.LONG,
                FuturesHedgePositionTransition.OrderSide.SELL, value("1"), value("110"), true, 18, RoundingMode.HALF_EVEN);
        var shortClose = FuturesHedgePositionTransition.apply(shortOpen.position(), FuturesPositionSide.SHORT,
                FuturesHedgePositionTransition.OrderSide.BUY, value("1"), value("110"), true, 18, RoundingMode.HALF_EVEN);
        assertEquals(0, longClose.realizedPnl().compareTo(value("10")));
        assertEquals(0, shortClose.realizedPnl().compareTo(value("-10")));
        assertEquals(0, longClose.position().quantity().compareTo(value("1")));
        assertEquals(0, shortClose.position().quantity().compareTo(value("1")));
    }

    @Test
    void weightedEntryAndFullCloseAreDeterministic() {
        var first = FuturesHedgePositionTransition.apply(null, FuturesPositionSide.LONG,
                FuturesHedgePositionTransition.OrderSide.BUY, value("1"), value("100"), false, 18, RoundingMode.HALF_EVEN);
        var second = FuturesHedgePositionTransition.apply(first.position(), FuturesPositionSide.LONG,
                FuturesHedgePositionTransition.OrderSide.BUY, value("1"), value("120"), false, 18, RoundingMode.HALF_EVEN);
        assertEquals(0, second.position().entryNotionalUsdt().compareTo(value("220")));
        var closed = FuturesHedgePositionTransition.apply(second.position(), FuturesPositionSide.LONG,
                FuturesHedgePositionTransition.OrderSide.SELL, value("2"), value("115"), true, 18, RoundingMode.HALF_EVEN);
        assertEquals(0, closed.realizedPnl().compareTo(value("10")));
        assertEquals(0, closed.position().quantity().signum());
        assertNull(closed.position().entryNotionalUsdt());
    }

    @Test
    void reduceOnlyAndOverCloseRejectRiskIncreasingFills() {
        var open = FuturesHedgePositionTransition.apply(null, FuturesPositionSide.SHORT,
                FuturesHedgePositionTransition.OrderSide.SELL, value("1"), value("100"), false, 18, RoundingMode.HALF_EVEN);
        assertThrows(IllegalArgumentException.class, () -> FuturesHedgePositionTransition.apply(open.position(),
                FuturesPositionSide.SHORT, FuturesHedgePositionTransition.OrderSide.SELL,
                value("1"), value("90"), true, 18, RoundingMode.HALF_EVEN));
        assertThrows(IllegalArgumentException.class, () -> FuturesHedgePositionTransition.apply(open.position(),
                FuturesPositionSide.SHORT, FuturesHedgePositionTransition.OrderSide.BUY,
                value("2"), value("90"), true, 18, RoundingMode.HALF_EVEN));
        assertThrows(IllegalArgumentException.class, () -> FuturesHedgePositionTransition.apply(open.position(),
                FuturesPositionSide.LONG, FuturesHedgePositionTransition.OrderSide.SELL,
                value("1"), value("90"), true, 18, RoundingMode.HALF_EVEN));
    }

    private static BigDecimal value(String value) { return new BigDecimal(value); }
}
