package com.example.exchange.domain.model.entity;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

/**
 * 交易對設定：撮合刻度、風控上限與費率。
 */
@Data
@Builder
@Jacksonized
public class SymbolConfig {

    private String symbol;
    private String baseAsset;
    private String quoteAsset;
    private BigDecimal priceTick;
    private BigDecimal lotSize;
    private BigDecimal minQty;
    private BigDecimal minNotional;
    private BigDecimal maxOrderNotional;
    private BigDecimal maxPositionNotional;
    private int maxLeverage;
    private BigDecimal makerFeeRate;
    private BigDecimal takerFeeRate;
    private BigDecimal makerRebateRate;
    private BigDecimal referralRebateRate;
    private BigDecimal priceBandRate;
    private BigDecimal maintenanceMarginRate;
    private boolean tradingEnabled;

    public Symbol toSymbol() {
        return Symbol.builder()
                .base(baseAsset)
                .quote(quoteAsset)
                .priceScale(scaleOf(priceTick))
                .qtyScale(scaleOf(lotSize))
                .build();
    }

    public BigDecimal priceTickOrDefault() {
        return defaultIfNull(priceTick, new BigDecimal("0.01"));
    }

    public BigDecimal lotSizeOrDefault() {
        return defaultIfNull(lotSize, new BigDecimal("0.001"));
    }

    public BigDecimal minQtyOrDefault() {
        return defaultIfNull(minQty, lotSizeOrDefault());
    }

    public BigDecimal minNotionalOrDefault() {
        return defaultIfNull(minNotional, BigDecimal.ZERO);
    }

    public BigDecimal maxOrderNotionalOrDefault() {
        return defaultIfNull(maxOrderNotional, new BigDecimal("1000000"));
    }

    public BigDecimal maxPositionNotionalOrDefault() {
        return defaultIfNull(maxPositionNotional, new BigDecimal("5000000"));
    }

    public BigDecimal makerFeeRateOrDefault() {
        return defaultIfNull(makerFeeRate, new BigDecimal("0.0002"));
    }

    public BigDecimal takerFeeRateOrDefault() {
        return defaultIfNull(takerFeeRate, new BigDecimal("0.0005"));
    }

    public BigDecimal makerRebateRateOrDefault() {
        return defaultIfNull(makerRebateRate, BigDecimal.ZERO);
    }

    public BigDecimal referralRebateRateOrDefault() {
        return defaultIfNull(referralRebateRate, BigDecimal.ZERO);
    }

    public BigDecimal priceBandRateOrDefault() {
        return defaultIfNull(priceBandRate, new BigDecimal("0.10"));
    }

    public BigDecimal maintenanceMarginRateOrDefault() {
        return defaultIfNull(maintenanceMarginRate, new BigDecimal("0.005"));
    }

    private static int scaleOf(BigDecimal value) {
        if (value == null) return 8;
        return Math.max(0, value.stripTrailingZeros().scale());
    }

    private static BigDecimal defaultIfNull(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }
}
