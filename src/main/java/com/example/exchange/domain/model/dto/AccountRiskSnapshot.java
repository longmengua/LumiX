/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountRiskSnapshot(
        long uid,
        BigDecimal crossBalance,
        BigDecimal availableBalance,
        BigDecimal orderHold,
        BigDecimal positionMargin,
        BigDecimal frozenFunds,
        BigDecimal unrealizedPnl,
        BigDecimal totalEquity,
        BigDecimal maintenanceMargin,
        BigDecimal riskRatio,
        int openPositionCount,
        Instant calculatedAt
) {}
