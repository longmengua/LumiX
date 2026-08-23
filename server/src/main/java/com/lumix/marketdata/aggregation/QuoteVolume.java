package com.lumix.marketdata.aggregation;

import com.lumix.marketdata.contract.AtomicQuantity;
import com.lumix.marketdata.contract.DecimalPrice;
import com.lumix.marketdata.contract.InstrumentPrecision;
import java.math.BigInteger;
import java.util.Objects;

/**
 * 以 priceScale + quantityScale 表示的 quote-volume 原子整數。
 *
 * <p>price 與 base quantity 先各自保留 T02 的 value object；只有在聚合時才相乘。乘積沒有自動 rounding，
 * 任一超過同一 instrument overflow 邊界的結果都由 reducer fail closed。</p>
 */
public record QuoteVolume(BigInteger atoms, int scale) {

    public QuoteVolume {
        atoms = Objects.requireNonNull(atoms, "atoms must not be null");
        if (atoms.signum() < 0 || scale < 0) {
            throw new IllegalArgumentException("quote volume must be non-negative with a non-negative scale");
        }
    }

    /**
     * 建立單筆 trade 的 quote volume；不能以 double 計算，否則精確 replay 會受 binary rounding 影響。
     */
    public static QuoteVolume forTrade(DecimalPrice price, AtomicQuantity quantity, InstrumentPrecision precision) {
        price = Objects.requireNonNull(price, "price must not be null");
        quantity = Objects.requireNonNull(quantity, "quantity must not be null");
        precision = Objects.requireNonNull(precision, "precision must not be null");
        int quoteScale = Math.addExact(precision.priceScale(), precision.quantityScale());
        BigInteger value = price.value().unscaledValue().multiply(quantity.atoms());
        requireWithinPrecision(value, precision);
        return new QuoteVolume(value, quoteScale);
    }

    /**
     * 累加相同 instrument precision 的 quote volume；不同 scale 代表不同數值語意，不能隱式轉換。
     */
    public QuoteVolume add(QuoteVolume other, InstrumentPrecision precision) {
        other = Objects.requireNonNull(other, "other must not be null");
        precision = Objects.requireNonNull(precision, "precision must not be null");
        if (scale != other.scale()) {
            throw new ArithmeticException("quote volume scale mismatch");
        }
        BigInteger sum = atoms.add(other.atoms());
        requireWithinPrecision(sum, precision);
        return new QuoteVolume(sum, scale);
    }

    private static void requireWithinPrecision(BigInteger value, InstrumentPrecision precision) {
        if (value.toString().length() > precision.maximumSignificantDigits()) {
            throw new ArithmeticException("quote volume exceeds instrument precision overflow boundary");
        }
    }
}
