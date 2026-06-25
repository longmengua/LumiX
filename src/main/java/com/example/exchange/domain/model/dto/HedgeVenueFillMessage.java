/*
 * 檔案用途：外部 hedge venue 成交回報的標準化 message。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class HedgeVenueFillMessage {

    private final String marketMakerId;

    private final String symbol;

    private final String venueOrderId;

    private final String venueFillId;

    private final OrderSide side;

    private final BigDecimal quantity;

    private final BigDecimal price;

    private final BigDecimal fee;

    private final String feeAsset;

    private final String refId;

    private final Instant filledAt;
    public HedgeVenueFillMessage(String marketMakerId, String symbol, String venueOrderId, String venueFillId, OrderSide side, BigDecimal quantity, BigDecimal price, BigDecimal fee, String feeAsset, String refId, Instant filledAt) {
        this.marketMakerId = marketMakerId;
        this.symbol = symbol;
        this.venueOrderId = venueOrderId;
        this.venueFillId = venueFillId;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.fee = fee;
        this.feeAsset = feeAsset;
        this.refId = refId;
        this.filledAt = filledAt;
    }

    public String marketMakerId() {
        return marketMakerId;
    }

    public String symbol() {
        return symbol;
    }

    public String venueOrderId() {
        return venueOrderId;
    }

    public String venueFillId() {
        return venueFillId;
    }

    public OrderSide side() {
        return side;
    }

    public BigDecimal quantity() {
        return quantity;
    }

    public BigDecimal price() {
        return price;
    }

    public BigDecimal fee() {
        return fee;
    }

    public String feeAsset() {
        return feeAsset;
    }

    public String refId() {
        return refId;
    }

    public Instant filledAt() {
        return filledAt;
    }
}