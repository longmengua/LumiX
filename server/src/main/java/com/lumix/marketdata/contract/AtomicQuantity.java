package com.lumix.marketdata.contract;

import java.math.BigInteger;

/**
 * 以最小單位原子整數表示的數量或成交量。
 *
 * <p>wire format 永遠是非負整數字串；quantity scale 由同一事件的 {@link InstrumentPrecision} 保存，
 * 因此不會因 locale 或 decimal formatter 改變值。</p>
 */
public record AtomicQuantity(BigInteger atoms) {

    public AtomicQuantity {
        MarketDataContractValidation.requireValue(atoms, "atoms");
        if (atoms.signum() < 0) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.INVALID_ATOMIC_INTEGER,
                    "atomic quantity must not be negative"
            );
        }
    }

    public static AtomicQuantity fromWire(String value, InstrumentPrecision precision) {
        MarketDataContractValidation.requireValue(precision, "precision");
        if (value == null || !MarketDataContractValidation.ATOMIC_INTEGER_PATTERN.matcher(value).matches()) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.INVALID_ATOMIC_INTEGER,
                    "atomic quantity must use a canonical non-negative integer string"
            );
        }
        if (value.length() > precision.maximumSignificantDigits()) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.NUMERIC_OVERFLOW,
                    "atomic quantity exceeds instrument precision overflow boundary"
            );
        }
        return new AtomicQuantity(new BigInteger(value));
    }

    public boolean isPositive() {
        return atoms.signum() > 0;
    }

    public String toWireString() {
        return atoms.toString();
    }
}
