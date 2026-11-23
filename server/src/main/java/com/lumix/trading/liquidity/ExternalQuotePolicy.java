package com.lumix.trading.liquidity;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 全站基準加幣別調整的版本化報價政策；百分比與每單位固定 USDT 可擇一使用或合併。
 * 此處只計算展示報價，不代表實際收費或資金入帳。
 * 版本與生效方式須由後台持久化政策提供，不能由行情 adapter 自行更改。
 */
public record ExternalQuotePolicy(long version, String asset, BigDecimal platformRate,
                                  BigDecimal platformFixedUsdt, BigDecimal assetRateAdjustment,
                                  BigDecimal assetFixedAdjustmentUsdt) {
    public ExternalQuotePolicy {
        if (version <= 0 || asset == null || !asset.matches("[A-Z0-9]{2,16}"))
            throw new IllegalArgumentException("invalid quote policy identity");
        platformRate = rate(platformRate);
        platformFixedUsdt = nonnegative(platformFixedUsdt);
        assetRateAdjustment = Objects.requireNonNull(assetRateAdjustment, "asset rate adjustment is required");
        assetFixedAdjustmentUsdt = Objects.requireNonNull(assetFixedAdjustmentUsdt, "asset fixed adjustment is required");
        if (assetRateAdjustment.scale() > 18 || assetRateAdjustment.precision() > 19
                || assetRateAdjustment.abs().compareTo(BigDecimal.ONE) > 0)
            throw new IllegalArgumentException("asset adjustment is out of bounds");
        BigDecimal effective = platformRate.add(assetRateAdjustment);
        BigDecimal fixed = platformFixedUsdt.add(assetFixedAdjustmentUsdt);
        if (effective.signum() < 0 || effective.compareTo(BigDecimal.ONE) >= 0
                || fixed.signum() < 0)
            throw new IllegalArgumentException("effective quote rate is out of bounds");
        nonnegative(fixed);
    }

    /** 負的幣別調整可折減基準費率，但最終報價率不得為負。 */
    public BigDecimal effectiveRate() {
        return platformRate.add(assetRateAdjustment);
    }

    /** 每一單位基礎幣的固定 USDT 價差；對買盤扣除，對賣盤加上。 */
    public BigDecimal effectiveFixedUsdt() {
        return platformFixedUsdt.add(assetFixedAdjustmentUsdt);
    }

    private static BigDecimal rate(BigDecimal value) {
        Objects.requireNonNull(value, "platform rate is required");
        if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) >= 0 || value.scale() > 18)
            throw new IllegalArgumentException("platform rate is out of bounds");
        return value;
    }

    private static BigDecimal nonnegative(BigDecimal value) {
        Objects.requireNonNull(value, "fixed quote amount is required");
        if (value.signum() < 0 || value.scale() > 18 || value.precision() > 36
                || value.precision() - (long) value.scale() > 18)
            throw new IllegalArgumentException("fixed quote amount is out of bounds");
        return value;
    }

}
