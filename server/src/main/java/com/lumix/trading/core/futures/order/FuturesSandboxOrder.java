package com.lumix.trading.core.futures.order;

import com.lumix.account.AccountId;
import com.lumix.common.RequestId;
import com.lumix.trading.core.futures.leverage.FuturesLeverage;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Futures sandbox accepted order 的 immutable 快照。
 *
 * 這個 snapshot 只表示 placement gate 已接受 order 進入 sandbox 範圍，不包含 fill、trade、position、
 * reservation、ledger、settlement 或 order-book sequence 資訊。
 */
public record FuturesSandboxOrder(
        FuturesOrderId orderId,
        RequestId requestId,
        AccountId futuresAccountId,
        FuturesMarketSymbol marketSymbol,
        FuturesOrderSide side,
        FuturesOrderType type,
        FuturesPositionQuantity quantity,
        FuturesEntryPrice limitPrice,
        FuturesTimeInForce timeInForce,
        FuturesLeverage leverage,
        Instant acceptedAt,
        FuturesOrderStatus status,
        Optional<String> clientOrderId
) {

    public FuturesSandboxOrder {
        // accepted order 必須是完整且可審計的 immutable snapshot，不能用半成品欄位假裝 order 已可進入後續 runtime。
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(futuresAccountId, "futuresAccountId must not be null");
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
        Objects.requireNonNull(side, "side must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(limitPrice, "limitPrice must not be null");
        Objects.requireNonNull(timeInForce, "timeInForce must not be null");
        Objects.requireNonNull(leverage, "leverage must not be null");
        Objects.requireNonNull(acceptedAt, "acceptedAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(clientOrderId, "clientOrderId must not be null");
        clientOrderId = normalizeOptionalClientOrderId(clientOrderId);
        if (status != FuturesOrderStatus.ACCEPTED_FOR_SANDBOX) {
            throw new IllegalArgumentException("accepted sandbox order status must be ACCEPTED_FOR_SANDBOX");
        }
    }

    private static Optional<String> normalizeOptionalClientOrderId(Optional<String> clientOrderId) {
        return clientOrderId.map(value -> {
            String normalized = value.trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("clientOrderId must not be blank when present");
            }
            return normalized;
        });
    }
}
