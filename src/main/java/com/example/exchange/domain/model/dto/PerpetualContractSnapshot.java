/*
 * 檔案用途：永續合約市場與風控快照 DTO，讓前端不用自行拼湊標記價格、資金費率或合約規格。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class PerpetualContractSnapshot {

    private final String symbol;

    private final String contractType;

    private final String baseAsset;

    private final String quoteAsset;

    private final BigDecimal contractSize;

    private final BigDecimal indexPrice;

    private final BigDecimal markPrice;

    private final BigDecimal fundingRate;

    private final Instant nextFundingTime;

    private final Integer maxLeverage;

    private final Integer defaultLeverage;

    private final String marginMode;

    private final BigDecimal initialMarginRate;

    private final BigDecimal maintenanceMarginRate;

    private final BigDecimal estimatedLiquidationPrice;

    private final String status;

    private final Instant updatedAt;
    public PerpetualContractSnapshot(String symbol, String contractType, String baseAsset, String quoteAsset, BigDecimal contractSize, BigDecimal indexPrice, BigDecimal markPrice, BigDecimal fundingRate, Instant nextFundingTime, Integer maxLeverage, Integer defaultLeverage, String marginMode, BigDecimal initialMarginRate, BigDecimal maintenanceMarginRate, BigDecimal estimatedLiquidationPrice, String status, Instant updatedAt) {
        this.symbol = symbol;
        this.contractType = contractType;
        this.baseAsset = baseAsset;
        this.quoteAsset = quoteAsset;
        this.contractSize = contractSize;
        this.indexPrice = indexPrice;
        this.markPrice = markPrice;
        this.fundingRate = fundingRate;
        this.nextFundingTime = nextFundingTime;
        this.maxLeverage = maxLeverage;
        this.defaultLeverage = defaultLeverage;
        this.marginMode = marginMode;
        this.initialMarginRate = initialMarginRate;
        this.maintenanceMarginRate = maintenanceMarginRate;
        this.estimatedLiquidationPrice = estimatedLiquidationPrice;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public String symbol() {
        return symbol;
    }

    public String contractType() {
        return contractType;
    }

    public String baseAsset() {
        return baseAsset;
    }

    public String quoteAsset() {
        return quoteAsset;
    }

    public BigDecimal contractSize() {
        return contractSize;
    }

    public BigDecimal indexPrice() {
        return indexPrice;
    }

    public BigDecimal markPrice() {
        return markPrice;
    }

    public BigDecimal fundingRate() {
        return fundingRate;
    }

    public Instant nextFundingTime() {
        return nextFundingTime;
    }

    public Integer maxLeverage() {
        return maxLeverage;
    }

    public Integer defaultLeverage() {
        return defaultLeverage;
    }

    public String marginMode() {
        return marginMode;
    }

    public BigDecimal initialMarginRate() {
        return initialMarginRate;
    }

    public BigDecimal maintenanceMarginRate() {
        return maintenanceMarginRate;
    }

    public BigDecimal estimatedLiquidationPrice() {
        return estimatedLiquidationPrice;
    }

    public String status() {
        return status;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}