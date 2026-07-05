package com.lumix.spot;

import com.lumix.account.UserId;
import com.lumix.common.MoneyAmount;
import com.lumix.common.RequestId;

import java.time.Instant;
import java.util.Objects;

/**
 * 現貨訂單展示模型。
 */
public record SpotOrderView(
        String orderId,
        RequestId requestId,
        UserId userId,
        String symbol,
        SpotOrderSide side,
        SpotOrderType type,
        MoneyAmount price,
        MoneyAmount quantity,
        MoneyAmount filledQuantity,
        SpotOrderStatus status,
        TimeInForce timeInForce,
        String clientOrderId,
        String statusNote,
        Instant createdAt,
        Instant updatedAt
) {

    public SpotOrderView {
        orderId = requireText(orderId, "orderId");
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        symbol = requireText(symbol, "symbol");
        Objects.requireNonNull(side, "side must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(filledQuantity, "filledQuantity must not be null");
        Objects.requireNonNull(status, "status must not be null");
        clientOrderId = normalizeOptionalText(clientOrderId);
        statusNote = normalizeOptionalText(statusNote);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (price != null && !price.isPositive()) {
            throw new IllegalArgumentException("price must be greater than zero when present");
        }
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (filledQuantity.isNegative()) {
            throw new IllegalArgumentException("filledQuantity must not be negative");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
