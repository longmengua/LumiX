/*
 * 檔案用途：做市商 hedge decision audit read model。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HedgeDecisionAuditRecord(
        UUID id,
        String marketMakerId,
        String symbol,
        boolean accepted,
        String reason,
        BigDecimal orderNotional,
        String venueOrderId,
        String refId,
        Instant decidedAt,
        Instant createdAt
) {
    public HedgeDecisionAuditRecord {
        id = id == null ? UUID.randomUUID() : id;
        orderNotional = orderNotional == null ? BigDecimal.ZERO : orderNotional;
        decidedAt = decidedAt == null ? Instant.now() : decidedAt;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
