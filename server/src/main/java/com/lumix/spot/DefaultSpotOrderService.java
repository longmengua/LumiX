package com.lumix.spot;

import com.lumix.account.UserId;
import com.lumix.common.BusinessException;
import com.lumix.common.ErrorCode;
import com.lumix.common.MoneyAmount;
import com.lumix.common.RequestId;
import com.lumix.ledger.LedgerService;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Phase 10 現貨訂單 stub。
 * 只做 request validation 與流程邊界保留，不做真實撮合、資產凍結或成交結算。
 */
public class DefaultSpotOrderService implements SpotOrderService {

    private final LedgerService ledgerService;
    private final MatchingEngineClient matchingEngineClient;

    public DefaultSpotOrderService(LedgerService ledgerService, MatchingEngineClient matchingEngineClient) {
        this.ledgerService = Objects.requireNonNull(ledgerService, "ledgerService must not be null");
        this.matchingEngineClient = Objects.requireNonNull(
                matchingEngineClient,
                "matchingEngineClient must not be null"
        );
    }

    @Override
    public SpotOrderView placeOrder(SpotOrderRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        validateRequest(request);

        // TODO: requires high-reasoning review before production use
        // Placeholder only. BUY orders should reserve quote asset via LedgerService after formal review.

        // TODO: requires high-reasoning review before production use
        // Placeholder only. SELL orders should reserve base asset via LedgerService after formal review.

        // TODO: requires high-reasoning review before production use
        // Placeholder only. Real order submission must go through MatchingEngineClient to a reviewed C++ Core.
        // This stub intentionally does not call matchingEngineClient.submitOrder(...).

        return new SpotOrderView(
                "stub-" + request.requestId().value(),
                request.requestId(),
                request.userId(),
                request.symbol(),
                request.side(),
                request.type(),
                request.price(),
                request.quantity(),
                MoneyAmount.zero(),
                SpotOrderStatus.REJECTED,
                request.timeInForce(),
                request.clientOrderId(),
                "Validated only. Phase 10 does not freeze assets or submit orders to the matching core.",
                Instant.now(),
                Instant.now()
        );
    }

    @Override
    public boolean cancelOrder(RequestId requestId, UserId userId, String orderId) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        validateText(orderId, "orderId");

        // TODO: requires high-reasoning review before production use
        // Placeholder only. Real cancellation must check order state, contact MatchingEngineClient, and release assets via LedgerService.
        return false;
    }

    @Override
    public Optional<SpotOrderView> getOrder(UserId userId, String orderId) {
        Objects.requireNonNull(userId, "userId must not be null");
        validateText(orderId, "orderId");
        return Optional.empty();
    }

    @Override
    public List<SpotOrderView> listOpenOrders(UserId userId, String symbol) {
        Objects.requireNonNull(userId, "userId must not be null");
        validateText(symbol, "symbol");
        return List.of();
    }

    @Override
    public List<SpotOrderView> listOrderHistory(UserId userId, String symbol) {
        Objects.requireNonNull(userId, "userId must not be null");
        validateText(symbol, "symbol");
        return List.of();
    }

    @Override
    public List<SpotTradeView> listTrades(UserId userId, String symbol) {
        Objects.requireNonNull(userId, "userId must not be null");
        validateText(symbol, "symbol");
        return List.of();
    }

    private void validateRequest(SpotOrderRequest request) {
        if (!request.quantity().isPositive()) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "Order quantity must be greater than zero");
        }
        if (request.type() == SpotOrderType.LIMIT) {
            if (request.price() == null || !request.price().isPositive()) {
                throw new BusinessException(ErrorCode.INVALID_AMOUNT, "Limit order price must be greater than zero");
            }
            if (request.timeInForce() == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Limit order timeInForce must not be null");
            }
        }
        if (request.type() == SpotOrderType.MARKET && request.price() != null && !request.price().isPositive()) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "Market order price must be positive when provided");
        }
    }

    private void validateText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
