/*
 * 檔案用途：account/position restore consistency issue DTO。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

public record AccountPositionConsistencyIssue(
        long uid,
        String symbol,
        String issueType,
        BigDecimal accountPositionMargin,
        BigDecimal openPositionMargin,
        String detail
) {
}
