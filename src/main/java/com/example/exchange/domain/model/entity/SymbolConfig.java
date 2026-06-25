/*
 * 檔案用途：領域模型或持久化實體，承載交易、帳戶、持倉與預測市場狀態。
 */
package com.example.exchange.domain.model.entity;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import com.example.exchange.domain.model.enums.ProductType;

/**
 * 交易對設定：撮合刻度、風控上限與費率。
 */
@Data
@Builder
@Jacksonized
public class SymbolConfig {

    private String symbol;
    private ProductType productType;
    private String baseAsset;
    private String quoteAsset;
    private BigDecimal priceTick;
    private BigDecimal lotSize;
    private BigDecimal minQty;
    private BigDecimal minNotional;
    private BigDecimal maxOrderNotional;
    private BigDecimal maxPositionNotional;
    private Integer maxOpenOrders;
    private int maxLeverage;
    private BigDecimal makerFeeRate;
    private BigDecimal takerFeeRate;
    private BigDecimal makerRebateRate;
    private BigDecimal referralRebateRate;
    private BigDecimal priceBandRate;
    private BigDecimal initialMarginRate;
    private BigDecimal maintenanceMarginRate;
    private List<RiskTier> riskTiers;
    private boolean tradingEnabled;

    public Symbol toSymbol() {
        return Symbol.builder()
                .code(symbol)
                .base(baseAsset)
                .quote(quoteAsset)
                .priceScale(scaleOf(priceTick))
                .qtyScale(scaleOf(lotSize))
                .build();
    }

    public BigDecimal priceTickOrDefault() {
        return defaultIfNull(priceTick, new BigDecimal("0.01"));
    }

    public ProductType productTypeOrDefault() {
        return productType == null ? ProductType.PERPETUAL : productType;
    }

    public boolean isSpot() {
        return productTypeOrDefault() == ProductType.SPOT;
    }

    public boolean isPerpetual() {
        return productTypeOrDefault() == ProductType.PERPETUAL;
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

    public int maxLeverageOrDefault() {
        return maxLeverage <= 0 ? 20 : maxLeverage;
    }

    public int maxOpenOrdersOrDefault() {
        return maxOpenOrders == null || maxOpenOrders <= 0 ? 200 : maxOpenOrders;
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

    public BigDecimal initialMarginRateOrDefault() {
        return defaultIfNull(initialMarginRate, leverageInitialMarginRate(maxLeverageOrDefault()));
    }

    public RiskTier riskTierForNotional(BigDecimal positionNotional) {
        BigDecimal notional = positionNotional == null ? BigDecimal.ZERO : positionNotional.abs();
        List<RiskTier> tiers = configuredRiskTiers();
        if (tiers.isEmpty()) {
            return defaultRiskTier();
        }
        return tiers.stream()
                .filter(tier -> tier.maxPositionNotionalOrDefault(maxPositionNotionalOrDefault()).compareTo(notional) >= 0)
                .min(Comparator.comparing(tier -> tier.maxPositionNotionalOrDefault(maxPositionNotionalOrDefault())))
                .orElse(null);
    }

    public BigDecimal initialMarginRateForNotional(BigDecimal positionNotional) {
        RiskTier tier = riskTierForNotional(positionNotional);
        return tier == null ? initialMarginRateOrDefault() : tier.initialMarginRateOrDefault(initialMarginRateOrDefault());
    }

    public BigDecimal maintenanceMarginRateForNotional(BigDecimal positionNotional) {
        RiskTier tier = riskTierForNotional(positionNotional);
        return tier == null ? maintenanceMarginRateOrDefault() : tier.maintenanceMarginRateOrDefault(maintenanceMarginRateOrDefault());
    }

    public int maxLeverageForNotional(BigDecimal positionNotional) {
        RiskTier tier = riskTierForNotional(positionNotional);
        return tier == null ? maxLeverageOrDefault() : tier.maxLeverageOrDefault(maxLeverageOrDefault());
    }

    private List<RiskTier> configuredRiskTiers() {
        if (riskTiers == null || riskTiers.isEmpty()) return List.of();
        return riskTiers.stream()
                .filter(tier -> tier != null
                        && tier.getMaxPositionNotional() != null
                        && tier.getMaxPositionNotional().signum() > 0)
                .toList();
    }

    private RiskTier defaultRiskTier() {
        return RiskTier.builder()
                .tier(1)
                .maxPositionNotional(maxPositionNotionalOrDefault())
                .initialMarginRate(initialMarginRateOrDefault())
                .maintenanceMarginRate(maintenanceMarginRateOrDefault())
                .maxLeverage(maxLeverageOrDefault())
                .build();
    }

    private static int scaleOf(BigDecimal value) {
        if (value == null) return 8;
        return Math.max(0, value.stripTrailingZeros().scale());
    }

    private static BigDecimal defaultIfNull(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private static BigDecimal leverageInitialMarginRate(int leverage) {
        return BigDecimal.ONE.divide(BigDecimal.valueOf(Math.max(1, leverage)), 18, RoundingMode.HALF_UP);
    }

    @Data
    @Builder
    @Jacksonized
    public static class RiskTier {
        private Integer tier;
        private BigDecimal maxPositionNotional;
        private BigDecimal initialMarginRate;
        private BigDecimal maintenanceMarginRate;
        private Integer maxLeverage;

        public BigDecimal maxPositionNotionalOrDefault(BigDecimal fallback) {
            return defaultIfNull(maxPositionNotional, fallback);
        }

        public BigDecimal initialMarginRateOrDefault(BigDecimal fallback) {
            return defaultIfNull(initialMarginRate, fallback);
        }

        public BigDecimal maintenanceMarginRateOrDefault(BigDecimal fallback) {
            return defaultIfNull(maintenanceMarginRate, fallback);
        }

        public int maxLeverageOrDefault(int fallback) {
            return maxLeverage == null || maxLeverage <= 0 ? fallback : maxLeverage;
        }
    }
}
