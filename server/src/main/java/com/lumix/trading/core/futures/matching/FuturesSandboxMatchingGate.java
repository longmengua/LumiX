package com.lumix.trading.core.futures.matching;

import com.lumix.trading.core.futures.order.FuturesOrderSide;
import com.lumix.trading.core.futures.order.FuturesOrderStatus;
import com.lumix.trading.core.futures.order.FuturesSandboxOrder;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.sandbox.matching.SandboxLimitOrderCandidate;
import com.lumix.trading.core.sandbox.matching.SandboxLimitOrderMatchPair;
import com.lumix.trading.core.sandbox.matching.SandboxLimitOrderMatchingPolicy;
import com.lumix.trading.core.sandbox.matching.SandboxLimitOrderSide;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Futures sandbox 對共用限價單候選配對規則的受限適配層。
 *
 * 此 gate 不保存 order、不遞迴執行 matching，也不產生 fill、trade、position、PnL、reservation、ledger 或 settlement side effect。
 */
public final class FuturesSandboxMatchingGate {

    private final SandboxLimitOrderMatchingPolicy matchingPolicy = new SandboxLimitOrderMatchingPolicy();

    /**
     * 評估同一 market 的 accepted futures order snapshots 是否存在最佳 crossed pair。
     *
     * T02 對輸入採取全量一致性檢查，避免呼叫端把多 market 資料混入單一 sandbox contract 而得到看似有效的配對結論。
     */
    public FuturesSandboxMatchingResult evaluate(
            FuturesMarketSymbol marketSymbol,
            List<FuturesSandboxOrder> acceptedOrders
    ) {
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
        Objects.requireNonNull(acceptedOrders, "acceptedOrders must not be null");

        if (acceptedOrders.stream().anyMatch(order -> order == null
                || order.status() != FuturesOrderStatus.ACCEPTED_FOR_SANDBOX
                || !marketSymbol.equals(order.marketSymbol()))) {
            return FuturesSandboxMatchingResult.rejected(FuturesSandboxMatchingReason.MARKET_MISMATCH);
        }

        Map<String, FuturesSandboxOrder> ordersById = new LinkedHashMap<>();
        for (FuturesSandboxOrder acceptedOrder : acceptedOrders) {
            if (ordersById.putIfAbsent(acceptedOrder.orderId().value(), acceptedOrder) != null) {
                return FuturesSandboxMatchingResult.rejected(FuturesSandboxMatchingReason.DUPLICATE_ORDER_ID);
            }
        }

        try {
            return matchingPolicy.selectBestCrossedPair(ordersById.values().stream()
                            .map(FuturesSandboxMatchingGate::toSharedCandidate)
                            .toList())
                    .map(matchPair -> toFuturesCandidate(matchPair, ordersById))
                    .map(FuturesSandboxMatchingResult::matchEligible)
                    .orElseGet(FuturesSandboxMatchingResult::noCross);
        } catch (IllegalArgumentException exception) {
            // 共用 policy 只會因重複 ID 或多市場集合拒絕；market 已在上方鎖定，故保留可審計的重複 ID 結果。
            return FuturesSandboxMatchingResult.rejected(FuturesSandboxMatchingReason.DUPLICATE_ORDER_ID);
        }
    }

    private static SandboxLimitOrderCandidate toSharedCandidate(FuturesSandboxOrder order) {
        return new SandboxLimitOrderCandidate(
                order.orderId().value(),
                order.marketSymbol().value(),
                order.side() == FuturesOrderSide.BUY ? SandboxLimitOrderSide.BUY : SandboxLimitOrderSide.SELL,
                order.limitPrice().value(),
                order.quantity().value(),
                order.acceptedAt()
        );
    }

    private static FuturesSandboxMatchCandidate toFuturesCandidate(
            SandboxLimitOrderMatchPair matchPair,
            Map<String, FuturesSandboxOrder> ordersById
    ) {
        return new FuturesSandboxMatchCandidate(
                ordersById.get(matchPair.buyOrder().orderId()),
                ordersById.get(matchPair.sellOrder().orderId()),
                ordersById.get(matchPair.makerOrder().orderId()),
                matchPair.matchedQuantity()
        );
    }
}
