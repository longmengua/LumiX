package com.lumix.trading.core.futures.order;

import com.lumix.account.AccountId;
import com.lumix.common.RequestId;
import com.lumix.trading.core.futures.leverage.IsolatedLeverageConfig;
import com.lumix.trading.core.futures.margin.IsolatedMarginCheckRequest;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Futures sandbox order placement 的 immutable 輸入快照。
 *
 * 這個 request 只保存 order placement 所需的明確輸入，並攜帶原始 margin proposal 供 gate 重新計算驗證；
 * 它不讀取外部狀態，也不嵌入 wallet、ledger、balance、order book 或 matching engine。
 */
public record FuturesOrderPlacementRequest(
        RequestId requestId,
        FuturesOrderId orderId,
        AccountId futuresAccountId,
        FuturesMarketSymbol marketSymbol,
        FuturesOrderSide side,
        FuturesOrderType type,
        FuturesPositionQuantity quantity,
        FuturesEntryPrice limitPrice,
        FuturesTimeInForce timeInForce,
        IsolatedLeverageConfig leverageConfig,
        IsolatedMarginCheckRequest marginCheckRequest,
        Instant submittedAt,
        Optional<String> clientOrderId
) {

    public FuturesOrderPlacementRequest {
        // T01 只接受已正規化完成的 immutable domain model，避免呼叫端用裸數值或外部 runtime 物件繞過邊界。
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(futuresAccountId, "futuresAccountId must not be null");
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
        Objects.requireNonNull(side, "side must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(limitPrice, "limitPrice must not be null");
        Objects.requireNonNull(timeInForce, "timeInForce must not be null");
        Objects.requireNonNull(leverageConfig, "leverageConfig must not be null");
        Objects.requireNonNull(marginCheckRequest, "marginCheckRequest must not be null");
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        Objects.requireNonNull(clientOrderId, "clientOrderId must not be null");

        clientOrderId = normalizeOptionalClientOrderId(clientOrderId);

        if (!futuresAccountId.equals(leverageConfig.futuresAccountId())) {
            throw new IllegalArgumentException("futuresAccountId must equal leverageConfig.futuresAccountId");
        }
        if (!marketSymbol.equals(leverageConfig.marketSymbol())) {
            throw new IllegalArgumentException("marketSymbol must equal leverageConfig.marketSymbol");
        }
        if (type != FuturesOrderType.LIMIT) {
            throw new IllegalArgumentException("type must be LIMIT");
        }
        if (timeInForce != FuturesTimeInForce.GTC) {
            throw new IllegalArgumentException("timeInForce must be GTC");
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
