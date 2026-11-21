package com.lumix.trading.runtime;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/**
 * 永續逐倉政策須明確指定保證金、行情、funding 與承擔穿倉的資金帳戶。
 * 所有數值由治理輸入，不從 sandbox 或外部交易所的政策自動複製。
 */
public record FuturesRiskPolicy(
        TradingExecutionPolicy execution,
        String collateralAsset,
        BigDecimal collateralHaircut,
        BigDecimal maximumLeverage,
        BigDecimal maintenanceMarginRate,
        BigDecimal maximumPositionNotionalUsdt,
        String markPriceSource,
        String indexPriceSource,
        Duration maximumPriceAge,
        Duration fundingInterval,
        BigDecimal maximumAbsoluteFundingRate,
        String insuranceAccountId,
        String conversionPoolAccountId
) {
    public FuturesRiskPolicy {
        Objects.requireNonNull(execution, "execution policy is required");
        if (execution.rules().kind() != TradingInstrumentRules.Kind.PERPETUAL
                || !"USDT".equals(execution.rules().quoteAsset())
                || execution.counterpartyModel() != TradingExecutionPolicy.CounterpartyModel.CLIENT_TO_CLIENT)
            throw new IllegalArgumentException("USDT client-to-client perpetual policy is required");
        collateralAsset = symbol(collateralAsset, "collateral asset");
        insuranceAccountId = symbol(insuranceAccountId, "insurance account");
        conversionPoolAccountId = symbol(conversionPoolAccountId, "conversion pool account");
        markPriceSource = source(markPriceSource, "mark price source");
        indexPriceSource = source(indexPriceSource, "index price source");
        if (insuranceAccountId.equals(conversionPoolAccountId))
            throw new IllegalArgumentException("insurance and conversion pool accounts must be separate");
        collateralHaircut = rate(collateralHaircut, "collateral haircut", false);
        maximumLeverage = positive(maximumLeverage, "maximum leverage");
        if (maximumLeverage.compareTo(BigDecimal.ONE) < 0)
            throw new IllegalArgumentException("maximum leverage must be at least one");
        maintenanceMarginRate = rate(maintenanceMarginRate, "maintenance margin rate", true);
        if (maintenanceMarginRate.multiply(maximumLeverage).compareTo(BigDecimal.ONE) >= 0)
            throw new IllegalArgumentException("maintenance margin must be below initial margin");
        maximumPositionNotionalUsdt = positive(maximumPositionNotionalUsdt, "position notional limit");
        maximumAbsoluteFundingRate = rate(maximumAbsoluteFundingRate, "funding rate cap", true);
        Objects.requireNonNull(maximumPriceAge, "price freshness limit is required");
        Objects.requireNonNull(fundingInterval, "funding interval is required");
        if (maximumPriceAge.isNegative() || maximumPriceAge.isZero()
                || fundingInterval.isNegative() || fundingInterval.isZero())
            throw new IllegalArgumentException("price age and funding interval must be positive");
    }

    private static String symbol(String value, String name) {
        if (value == null || value.length() > 64 || !value.matches("[A-Za-z0-9:_-]+"))
            throw new IllegalArgumentException(name + " is invalid");
        return value;
    }

    private static String source(String value, String name) {
        if (value == null || !value.matches("[A-Za-z0-9:._/-]{1,128}"))
            throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static BigDecimal positive(BigDecimal value, String name) {
        Objects.requireNonNull(value, name + " is required");
        TradingDecimal.ledgerAmount(value);
        if (value.signum() <= 0) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    private static BigDecimal rate(BigDecimal value, String name, boolean positive) {
        Objects.requireNonNull(value, name + " is required");
        TradingDecimal.ledgerAmount(value);
        if (value.compareTo(BigDecimal.ONE) >= 0 || value.signum() < 0 || (positive && value.signum() == 0))
            throw new IllegalArgumentException(name + " must be within policy bounds");
        return value;
    }
}
