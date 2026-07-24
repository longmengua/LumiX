package com.lumix.marketdata.contract;

/**
 * event 內保存的 instrument 精度 profile。
 *
 * <p>profile 與事件一起傳遞，讓 replay 與 serialization 不必從可變的外部 instrument 設定猜測 scale。
 * 最大有效位數是 contract 的 overflow 邊界，不是自動 rounding 規則。</p>
 */
public record InstrumentPrecision(int priceScale, int quantityScale, int maximumSignificantDigits) {

    private static final int MAX_SCALE = 18;
    private static final int MAX_SIGNIFICANT_DIGITS = 38;

    public InstrumentPrecision {
        if (priceScale < 0 || priceScale > MAX_SCALE || quantityScale < 0 || quantityScale > MAX_SCALE) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.SCALE_MISMATCH,
                    "priceScale and quantityScale must be between 0 and " + MAX_SCALE
            );
        }
        if (maximumSignificantDigits < 1 || maximumSignificantDigits > MAX_SIGNIFICANT_DIGITS) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.NUMERIC_OVERFLOW,
                    "maximumSignificantDigits must be between 1 and " + MAX_SIGNIFICANT_DIGITS
            );
        }
    }
}
