package com.lumix.spot;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.common.MoneyAmount;

import java.time.Instant;
import java.util.Objects;

/**
 * 現貨成交展示模型。
 */
public record SpotTradeView(
        String tradeId,
        String orderId,
        UserId userId,
        String symbol,
        SpotOrderSide side,
        MoneyAmount price,
        MoneyAmount quantity,
        MoneyAmount fee,
        AssetSymbol feeAsset,
        Instant executedAt
) {

    public SpotTradeView {
        tradeId = requireText(tradeId, "tradeId");
        orderId = requireText(orderId, "orderId");
        Objects.requireNonNull(userId, "userId must not be null");
        symbol = requireText(symbol, "symbol");
        Objects.requireNonNull(side, "side must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(executedAt, "executedAt must not be null");
        if (!price.isPositive()) {
            throw new IllegalArgumentException("price must be greater than zero");
        }
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (fee != null && fee.isNegative()) {
            throw new IllegalArgumentException("fee must not be negative");
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
}
