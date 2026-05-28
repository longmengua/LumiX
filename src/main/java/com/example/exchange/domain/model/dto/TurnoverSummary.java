/*
 * 檔案用途：流水統計 DTO，用於對帳與活動門檻查詢。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

/**
 * 聚合後的 turnover 結果。
 */
public record TurnoverSummary(
        long uid,
        String symbol,
        String strategyId,
        String marketMakerId,
        long tradeCount,
        BigDecimal quantity,
        BigDecimal notional
) {
}
