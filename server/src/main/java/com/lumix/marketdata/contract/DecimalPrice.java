package com.lumix.marketdata.contract;

import java.math.BigDecimal;

/**
 * 以 plain-string 建立、且已符合 instrument price scale 的價格值。
 *
 * <p>不接受 scientific notation 或隱式 scale 轉換，因為任何自動 rounding 都會破壞上游事件的可重放性。</p>
 */
public record DecimalPrice(BigDecimal value) {

    public DecimalPrice {
        MarketDataContractValidation.requireValue(value, "price");
        // BigDecimal 的負 scale 只能由 scientific notation 等非 plain 表示產生，不能繞過 wire factory 混入事件。
        if (value.scale() < 0) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.INVALID_DECIMAL,
                    "price must not use a negative scale"
            );
        }
    }

    public static DecimalPrice fromWire(String value, InstrumentPrecision precision) {
        MarketDataContractValidation.requireValue(precision, "precision");
        if (value == null || !MarketDataContractValidation.PLAIN_DECIMAL_PATTERN.matcher(value).matches()) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.INVALID_DECIMAL,
                    "price must use a non-negative plain decimal string"
            );
        }
        BigDecimal decimal = new BigDecimal(value);
        if (decimal.scale() != precision.priceScale()) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.SCALE_MISMATCH,
                    "price scale does not match instrument precision"
            );
        }
        if (decimal.precision() > precision.maximumSignificantDigits()) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.NUMERIC_OVERFLOW,
                    "price exceeds instrument precision overflow boundary"
            );
        }
        return new DecimalPrice(decimal);
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    public String toWireString() {
        return value.toPlainString();
    }
}
