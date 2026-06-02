/*
 * File purpose: Admin market-config response DTOs for read-only operations screens.
 */
package com.example.exchange.interfaces.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminMarketConfigResponse(
        List<MarketConfigItem> markets,
        MarketConfigCapabilities capabilities
) {

    public record MarketConfigItem(
            String symbol,
            String baseAsset,
            String quoteAsset,
            String status,
            BigDecimal priceTick,
            BigDecimal lotSize,
            BigDecimal minQty,
            BigDecimal minNotional,
            BigDecimal maxOrderNotional,
            BigDecimal maxPositionNotional,
            Integer maxOpenOrders,
            int maxLeverage,
            BigDecimal makerFeeRate,
            BigDecimal takerFeeRate,
            BigDecimal priceBandRate,
            BigDecimal initialMarginRate,
            BigDecimal maintenanceMarginRate,
            String tradingMode,
            String sessionWindow,
            boolean matchingEnabled,
            boolean marketDataEnabled,
            boolean manualSuspensionEnabled,
            List<RiskTierItem> riskTiers
    ) {
    }

    public record RiskTierItem(
            Integer tier,
            BigDecimal maxPositionNotional,
            BigDecimal initialMarginRate,
            BigDecimal maintenanceMarginRate,
            Integer maxLeverage
    ) {
    }

    public record MarketConfigCapabilities(
            boolean readOnly,
            boolean writesEnabled,
            List<String> disabledActions,
            List<String> requiredWriteEndpoints
    ) {
    }
}
