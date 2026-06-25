/*
 * 檔案用途：做市商 quote command 的風控決策結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketMakerQuoteDecision {

    private final String marketMakerId;

    private final String symbol;

    private final boolean accepted;

    private final String reason;

    private final Instant decidedAt;
    public MarketMakerQuoteDecision(String marketMakerId, String symbol, boolean accepted, String reason, Instant decidedAt) {
        this.marketMakerId = marketMakerId;
        this.symbol = symbol;
        this.accepted = accepted;
        this.reason = reason;
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

    public Instant decidedAt() {
        return decidedAt;
    }
}