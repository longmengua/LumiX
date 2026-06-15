/*
 * 檔案用途：永續合約市場與風控快照 DTO，讓前端不用自行拼湊標記價格、資金費率或合約規格。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PerpetualContractSnapshot(
        String symbol,
        String contractType,
        String baseAsset,
        String quoteAsset,
        BigDecimal contractSize,
        BigDecimal indexPrice,
        BigDecimal markPrice,
        BigDecimal fundingRate,
        Instant nextFundingTime,
        Integer maxLeverage,
        Integer defaultLeverage,
        String marginMode,
        BigDecimal initialMarginRate,
        BigDecimal maintenanceMarginRate,
        BigDecimal estimatedLiquidationPrice,
        String status,
        Instant updatedAt
) {}
