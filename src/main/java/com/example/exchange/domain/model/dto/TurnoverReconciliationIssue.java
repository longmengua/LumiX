/*
 * 檔案用途：Turnover 與 trade tape 對帳 issue DTO。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TurnoverReconciliationIssue(
        String code,
        String message,
        long uid,
        String matchId,
        UUID orderId,
        String strategyId,
        String marketMakerId,
        boolean ledgerRefPresent,
        BigDecimal turnoverQuantity,
        BigDecimal tradeTapeQuantity,
        BigDecimal turnoverNotional,
        BigDecimal tradeTapeNotional
) {
}
