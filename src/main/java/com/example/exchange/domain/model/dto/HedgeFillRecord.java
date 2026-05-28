/*
 * 檔案用途：做市商 hedge fill audit read model。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HedgeFillRecord(
        UUID id,
        String marketMakerId,
        String symbol,
        String venueOrderId,
        String venueFillId,
        OrderSide side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fee,
        String feeAsset,
        String refId,
        Instant filledAt,
        Instant createdAt
) {
    public HedgeFillRecord {
        id = id == null ? UUID.randomUUID() : id;
        quantity = quantity == null ? BigDecimal.ZERO : quantity;
        price = price == null ? BigDecimal.ZERO : price;
        fee = fee == null ? BigDecimal.ZERO : fee;
        filledAt = filledAt == null ? Instant.now() : filledAt;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public BigDecimal notional() {
        return quantity.abs().multiply(price);
    }
}
