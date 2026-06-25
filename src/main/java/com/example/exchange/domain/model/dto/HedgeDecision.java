/*
 * 檔案用途：對沖決策結果，保留風控拒絕原因與 venue 送單結果。
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
public class HedgeDecision {

    private final String marketMakerId;

    private final String symbol;

    private final boolean accepted;

    private final String reason;

    private final BigDecimal orderNotional;

    private final HedgeOrderResult orderResult;

    private final Instant decidedAt;
    public HedgeDecision(String marketMakerId, String symbol, boolean accepted, String reason, BigDecimal orderNotional, HedgeOrderResult orderResult, Instant decidedAt) {
        this.marketMakerId = marketMakerId;
        this.symbol = symbol;
        this.accepted = accepted;
        this.reason = reason;
        this.orderNotional = orderNotional;
        this.orderResult = orderResult;
        this.decidedAt = decidedAt;
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

    public HedgeOrderResult orderResult() {
        return orderResult;
    }

    public Instant decidedAt() {
        return decidedAt;
    }
}