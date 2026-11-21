package com.lumix.trading.core.futures.margin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * 其他幣別保證金以具名行情與後台核定折算率換算為 USDT 風險額度。
 * 此數值只用於風險判斷；已實現盈虧與 funding 仍須由帳本以 USDT 結算。
 */
public final class FuturesCollateralValuation {
    private FuturesCollateralValuation() { }

    /** 政策版本和來源時間須與風險決策一起保存，以便重放原決策。 */
    public record Rate(String collateralAsset, String source, long policyVersion,
                       BigDecimal usdtPerAsset, BigDecimal haircut,
                       Instant observedAt, Instant expiresAt) {
        public Rate {
            if (collateralAsset == null || !collateralAsset.matches("[A-Z0-9]{2,16}"))
                throw new IllegalArgumentException("collateral asset is invalid");
            if (source == null || source.isBlank() || source.length() > 128)
                throw new IllegalArgumentException("rate source is required");
            if (policyVersion <= 0) throw new IllegalArgumentException("rate policy version is invalid");
            Objects.requireNonNull(usdtPerAsset, "conversion rate is required");
            Objects.requireNonNull(haircut, "haircut is required");
            if (usdtPerAsset.signum() <= 0 || usdtPerAsset.scale() < 0
                    || usdtPerAsset.scale() > 18 || usdtPerAsset.precision() > 36)
                throw new IllegalArgumentException("conversion rate is invalid");
            if (haircut.signum() < 0 || haircut.compareTo(BigDecimal.ONE) >= 0
                    || haircut.scale() < 0 || haircut.scale() > 18)
                throw new IllegalArgumentException("haircut is invalid");
            Objects.requireNonNull(observedAt, "rate observation time is required");
            Objects.requireNonNull(expiresAt, "rate expiry time is required");
            if (!expiresAt.isAfter(observedAt)) throw new IllegalArgumentException("rate expiry must follow observation");
        }
    }

    /** 缺來源、過期或負抵押品時拒絕，不沿用舊價或抬高可用保證金。 */
    public static BigDecimal valueInUsdt(BigDecimal amount, Rate rate, Instant now, int usdtScale) {
        Objects.requireNonNull(amount, "collateral amount is required");
        Objects.requireNonNull(rate, "conversion rate is required");
        Objects.requireNonNull(now, "valuation time is required");
        if (amount.signum() < 0 || amount.scale() < 0 || amount.scale() > 18 || amount.precision() > 36)
            throw new IllegalArgumentException("collateral amount is invalid");
        if (now.isBefore(rate.observedAt()) || !now.isBefore(rate.expiresAt()))
            throw new IllegalArgumentException("conversion rate is stale");
        if (usdtScale < 0 || usdtScale > 18) throw new IllegalArgumentException("USDT scale is invalid");
        BigDecimal value = amount.multiply(rate.usdtPerAsset())
                .multiply(BigDecimal.ONE.subtract(rate.haircut()))
                .setScale(usdtScale, RoundingMode.DOWN);
        if (value.precision() > 36) throw new IllegalArgumentException("converted collateral exceeds ledger precision");
        return value;
    }
}
