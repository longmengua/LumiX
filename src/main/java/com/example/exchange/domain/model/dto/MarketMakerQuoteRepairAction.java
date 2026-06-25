/*
 * 檔案用途：記錄自動修復做市商 quote/open-order 對帳差異時採取的單一步驟。
 */
package com.example.exchange.domain.model.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketMakerQuoteRepairAction {

    private final String marketMakerId;

    private final long uid;

    private final String symbol;

    private final String side;

    private final UUID orderId;

    private final String clientOrderId;

    private final String action;

    private final String reason;

    private final boolean success;
    public MarketMakerQuoteRepairAction(String marketMakerId, long uid, String symbol, String side, UUID orderId, String clientOrderId, String action, String reason, boolean success) {
        this.marketMakerId = marketMakerId;
        this.uid = uid;
        this.symbol = symbol;
        this.side = side;
        this.orderId = orderId;
        this.clientOrderId = clientOrderId;
        this.action = action;
        this.reason = reason;
        this.success = success;
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

    public String action() {
        return action;
    }

    public String reason() {
        return reason;
    }

    public boolean success() {
        return success;
    }
}