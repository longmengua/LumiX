/*
 * 檔案用途：做市商 active quote state 與 open order 對帳差異項目。
 */
package com.example.exchange.domain.model.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketMakerQuoteReconciliationIssue {

    private final String marketMakerId;

    private final long uid;

    private final String symbol;

    private final String side;

    private final UUID orderId;

    private final String clientOrderId;

    private final String reason;
    public MarketMakerQuoteReconciliationIssue(String marketMakerId, long uid, String symbol, String side, UUID orderId, String clientOrderId, String reason) {
        this.marketMakerId = marketMakerId;
        this.uid = uid;
        this.symbol = symbol;
        this.side = side;
        this.orderId = orderId;
        this.clientOrderId = clientOrderId;
        this.reason = reason;
    }

    public String marketMakerId() {
        return marketMakerId;
    }

    public long uid() {
        return uid;
    }

    public String symbol() {
        return symbol;
    }

    public String side() {
        return side;
    }

    public UUID orderId() {
        return orderId;
    }

    public String clientOrderId() {
        return clientOrderId;
    }

    public String reason() {
        return reason;
    }
}