package com.lumix.spot;

import com.lumix.account.UserId;
import com.lumix.common.MoneyAmount;
import com.lumix.common.RequestId;

import java.util.Objects;

/**
 * 現貨下單請求模型。
 */
public record SpotOrderRequest(
        RequestId requestId,
        UserId userId,
        String symbol,
        SpotOrderSide side,
        SpotOrderType type,
        MoneyAmount price,
        MoneyAmount quantity,
        TimeInForce timeInForce,
        String clientOrderId
) {

    public SpotOrderRequest {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        symbol = requireText(symbol, "symbol");
        Objects.requireNonNull(side, "side must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        clientOrderId = normalizeOptionalText(clientOrderId);
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
