/*
 * 檔案用途：對沖決策結果，保留風控拒絕原因與 venue 送單結果。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HedgeDecision(
        String marketMakerId,
        String symbol,
        boolean accepted,
        String reason,
        BigDecimal orderNotional,
        HedgeOrderResult orderResult,
        Instant decidedAt
) {
}
