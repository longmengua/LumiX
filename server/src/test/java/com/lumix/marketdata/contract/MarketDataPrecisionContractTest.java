package com.lumix.marketdata.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * 驗證價格與數量 wire representation 不會發生隱式 rounding 或數值猜測。
 */
class MarketDataPrecisionContractTest {

    /**
     * decimal 必須保留 plain-string 與指定 scale；少位或 scientific notation 都不能被自動修正。
     */
    @Test
    void decimalUsesExactPlainStringAndScale() {
        InstrumentPrecision precision = new InstrumentPrecision(4, 8, 20);
        DecimalPrice price = DecimalPrice.fromWire("123.4500", precision);

        assertEquals("123.4500", price.toWireString());
        assertReason(MarketDataRejectionReason.SCALE_MISMATCH, () -> DecimalPrice.fromWire("123.45", precision));
        assertReason(MarketDataRejectionReason.INVALID_DECIMAL, () -> DecimalPrice.fromWire("1E3", precision));
        assertReason(MarketDataRejectionReason.INVALID_DECIMAL, () -> new DecimalPrice(new BigDecimal("1E3")));
    }

    /**
     * atomic quantity 僅接受 canonical integer string；保留前導零或超過 profile 邊界時必須 fail closed。
     */
    @Test
    void atomicQuantityUsesCanonicalIntegerStringAndChecksOverflow() {
        InstrumentPrecision precision = new InstrumentPrecision(2, 8, 5);
        AtomicQuantity quantity = AtomicQuantity.fromWire("12345", precision);

        assertEquals("12345", quantity.toWireString());
        assertReason(MarketDataRejectionReason.INVALID_ATOMIC_INTEGER, () -> AtomicQuantity.fromWire("001", precision));
        assertReason(MarketDataRejectionReason.NUMERIC_OVERFLOW, () -> AtomicQuantity.fromWire("123456", precision));
    }

    /**
     * BigDecimal 雖可承載任意大小數值，contract 仍以 instrument profile 建立明確 overflow boundary。
     */
    @Test
    void rejectsDecimalBeyondInstrumentOverflowBoundary() {
        InstrumentPrecision precision = new InstrumentPrecision(2, 8, 5);

        assertReason(MarketDataRejectionReason.NUMERIC_OVERFLOW, () -> DecimalPrice.fromWire("1234.56", precision));
    }

    private static void assertReason(MarketDataRejectionReason expected, org.junit.jupiter.api.function.Executable executable) {
        assertEquals(expected, assertThrows(MarketDataContractViolation.class, executable).reason());
    }
}
