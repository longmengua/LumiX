/*
 * 檔案用途：做市商 hedge fill audit read model。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class HedgeFillRecord {

    private final UUID id;

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

    private final String ledgerRefId;

    private final Instant filledAt;

    private final Instant createdAt;
    public HedgeFillRecord(UUID id, String marketMakerId, String symbol, String venueOrderId, String venueFillId, OrderSide side, BigDecimal quantity, BigDecimal price, BigDecimal fee, String feeAsset, String refId, String ledgerRefId, Instant filledAt, Instant createdAt) {
        id = id == null ? UUID.randomUUID() : id;
        quantity = quantity == null ? BigDecimal.ZERO : quantity;
        price = price == null ? BigDecimal.ZERO : price;
        fee = fee == null ? BigDecimal.ZERO : fee;
        filledAt = filledAt == null ? Instant.now() : filledAt;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    
        this.id = id;
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
        this.ledgerRefId = ledgerRefId;
        this.filledAt = filledAt;
        this.createdAt = createdAt;
    }

    public BigDecimal notional() {
        return quantity.abs().multiply(price);
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

    public String ledgerRefId() {
        return ledgerRefId;
    }

    public Instant filledAt() {
        return filledAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}