/*
 * 檔案用途：做市商自動報價 DTO，描述單一 profile/symbol 的策略執行結果。
 */
package com.example.exchange.domain.model.dto;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketMakerAutoQuoteResult {

    private final String marketMakerId;

    private final String symbol;

    private final boolean placed;

    private final String reason;

    private final String refId;
    public MarketMakerAutoQuoteResult(String marketMakerId, String symbol, boolean placed, String reason, String refId) {
        this.marketMakerId = marketMakerId;
        this.symbol = symbol;
        this.placed = placed;
        this.reason = reason;
        this.refId = refId;
    }

    public String marketMakerId() {
        return marketMakerId;
    }

    public String symbol() {
        return symbol;
    }

    public boolean placed() {
        return placed;
    }

    public String reason() {
        return reason;
    }

    public String refId() {
        return refId;
    }
}