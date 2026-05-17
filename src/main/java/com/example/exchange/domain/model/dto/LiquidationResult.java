/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LiquidationResult(
        long uid,
        String symbol,
        boolean liquidated,
        BigDecimal markPrice,
        BigDecimal maintenanceMargin,
        BigDecimal equity,
        BigDecimal closedQty,
        BigDecimal realizedPnl,
        BigDecimal insuranceFundCovered,
        BigDecimal adlCovered,
        String liquidationId,
        Instant ts
) {}
