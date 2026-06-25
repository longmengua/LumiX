/*
 * 檔案用途：做市商每個 symbol 的最新 quote ownership 狀態，用於重啟後查回 active quote orders。
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
public class MarketMakerQuoteState {

    private final String marketMakerId;

    private final long uid;

    private final String symbol;

    private final String refId;

    private final boolean active;

    private final boolean accepted;

    private final String reason;

    private final int canceledCount;

    private final BigDecimal bidPrice;

    private final BigDecimal bidQuantity;

    private final BigDecimal askPrice;

    private final BigDecimal askQuantity;

    private final UUID bidOrderId;

    private final UUID askOrderId;

    private final long bidVersion;

    private final long askVersion;

    private final UUID replacedBidOrderId;

    private final UUID replacedAskOrderId;

    private final Instant updatedAt;
    public MarketMakerQuoteState(String marketMakerId, long uid, String symbol, String refId, boolean active, boolean accepted, String reason, int canceledCount, BigDecimal bidPrice, BigDecimal bidQuantity, BigDecimal askPrice, BigDecimal askQuantity, UUID bidOrderId, UUID askOrderId, long bidVersion, long askVersion, UUID replacedBidOrderId, UUID replacedAskOrderId, Instant updatedAt) {
        this.marketMakerId = marketMakerId;
        this.uid = uid;
        this.symbol = symbol;
        this.refId = refId;
        this.active = active;
        this.accepted = accepted;
        this.reason = reason;
        this.canceledCount = canceledCount;
        this.bidPrice = bidPrice;
        this.bidQuantity = bidQuantity;
        this.askPrice = askPrice;
        this.askQuantity = askQuantity;
        this.bidOrderId = bidOrderId;
        this.askOrderId = askOrderId;
        this.bidVersion = bidVersion;
        this.askVersion = askVersion;
        this.replacedBidOrderId = replacedBidOrderId;
        this.replacedAskOrderId = replacedAskOrderId;
        this.updatedAt = updatedAt;
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

    public String refId() {
        return refId;
    }

    public boolean active() {
        return active;
    }

    public boolean accepted() {
        return accepted;
    }

    public String reason() {
        return reason;
    }

    public int canceledCount() {
        return canceledCount;
    }

    public BigDecimal bidPrice() {
        return bidPrice;
    }

    public BigDecimal bidQuantity() {
        return bidQuantity;
    }

    public BigDecimal askPrice() {
        return askPrice;
    }

    public BigDecimal askQuantity() {
        return askQuantity;
    }

    public UUID bidOrderId() {
        return bidOrderId;
    }

    public UUID askOrderId() {
        return askOrderId;
    }

    public long bidVersion() {
        return bidVersion;
    }

    public long askVersion() {
        return askVersion;
    }

    public UUID replacedBidOrderId() {
        return replacedBidOrderId;
    }

    public UUID replacedAskOrderId() {
        return replacedAskOrderId;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}