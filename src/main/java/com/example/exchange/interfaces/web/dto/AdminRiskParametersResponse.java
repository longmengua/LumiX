/*
 * File purpose: Admin risk-parameter response DTOs for read-only operations screens.
 */
package com.example.exchange.interfaces.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminRiskParametersResponse(
        RiskSwitches switches,
        List<RiskSymbolParameter> symbols,
        RiskParameterCapabilities capabilities
) {

    public record RiskSwitches(
            boolean orderEntryHalt,
            boolean reduceOnlyMode,
            boolean withdrawalHalt,
            boolean liquidationHalt,
            boolean liquidationManualReview,
            int liquidationScanBatchSize,
            boolean marketMakerHedgeExecutionHalt,
            List<String> suspendedSymbols,
            boolean orderEntryFrequencyLimitEnabled,
            int orderEntryFrequencyLimitMaxOrders,
            long orderEntryFrequencyLimitWindowSeconds
    ) {
    }

    public record RiskSymbolParameter(
            String symbol,
            String status,
            boolean suspended,
            int maxLeverage,
            BigDecimal maxOrderNotional,
            BigDecimal maxPositionNotional,
            BigDecimal initialMarginRate,
            BigDecimal maintenanceMarginRate,
            BigDecimal priceBandRate,
            OracleState oracle,
            List<RiskTierParameter> riskTiers
    ) {
    }

    public record RiskTierParameter(
            Integer tier,
            BigDecimal maxPositionNotional,
            BigDecimal initialMarginRate,
            BigDecimal maintenanceMarginRate,
            Integer maxLeverage
    ) {
    }

    public record OracleState(
            String status,
            BigDecimal markPrice,
            BigDecimal indexPrice,
            String source,
            Instant updatedAt,
            boolean stale
    ) {
    }

    public record RiskParameterCapabilities(
            boolean readOnly,
            boolean writesEnabled,
            List<String> disabledActions,
            List<String> requiredWriteEndpoints
    ) {
    }
}
