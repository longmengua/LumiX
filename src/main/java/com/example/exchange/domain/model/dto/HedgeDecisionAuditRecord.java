/*
 * 檔案用途：做市商 hedge decision audit read model。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class HedgeDecisionAuditRecord {

    private final UUID id;

    private final String marketMakerId;

    private final String symbol;

    private final boolean accepted;

    private final String reason;

    private final BigDecimal orderNotional;

    private final String venueOrderId;

    private final String refId;

    private final String internalTradeRefId;

    private final Instant decidedAt;

    private final Instant createdAt;
    public HedgeDecisionAuditRecord(UUID id, String marketMakerId, String symbol, boolean accepted, String reason, BigDecimal orderNotional, String venueOrderId, String refId, String internalTradeRefId, Instant decidedAt, Instant createdAt) {
        id = id == null ? UUID.randomUUID() : id;
        orderNotional = orderNotional == null ? BigDecimal.ZERO : orderNotional;
        decidedAt = decidedAt == null ? Instant.now() : decidedAt;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    
        this.id = id;
        this.marketMakerId = marketMakerId;
        this.symbol = symbol;
        this.accepted = accepted;
        this.reason = reason;
        this.orderNotional = orderNotional;
        this.venueOrderId = venueOrderId;
        this.refId = refId;
        this.internalTradeRefId = internalTradeRefId;
        this.decidedAt = decidedAt;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public String marketMakerId() {
        return marketMakerId;
    }

    public String symbol() {
        return symbol;
    }

    public boolean accepted() {
        return accepted;
    }

    public String reason() {
        return reason;
    }

    public BigDecimal orderNotional() {
        return orderNotional;
    }

    public String venueOrderId() {
        return venueOrderId;
    }

    public String refId() {
        return refId;
    }

    public String internalTradeRefId() {
        return internalTradeRefId;
    }

    public Instant decidedAt() {
        return decidedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}