package com.lumix.trading.core.spot.matching;

import com.lumix.trading.core.spot.orderbook.InMemorySpotSandboxOrderBook;
import com.lumix.trading.core.spot.orderbook.SpotSandboxOrderRecord;
import com.lumix.trading.core.spot.orderbook.SpotSandboxOrderStatus;
import com.lumix.trading.core.spot.orderintake.SpotOrderSide;
import com.lumix.trading.core.spot.orderintake.SpotOrderType;
import com.lumix.trading.core.spot.orderintake.SpotTimeInForce;
import com.lumix.trading.core.sandbox.matching.SandboxLimitOrderCandidate;
import com.lumix.trading.core.sandbox.matching.SandboxLimitOrderMatchPair;
import com.lumix.trading.core.sandbox.matching.SandboxLimitOrderMatchingPolicy;
import com.lumix.trading.core.sandbox.matching.SandboxLimitOrderSide;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * In-memory Spot sandbox matcher。
 *
 * 這個 matcher 只在記憶體內依照 sandbox rule 產生 trade / fill 與 settlement input，不代表正式 matching runtime 已完成。
 */
public final class InMemorySpotSandboxMatcher {

    private final InMemorySpotSandboxOrderBook orderBook;
    private final SpotSandboxTradeFillBoundary tradeFillBoundary;
    private final SandboxLimitOrderMatchingPolicy matchingPolicy = new SandboxLimitOrderMatchingPolicy();

    /**
     * 建立 sandbox matcher。
     *
     * 這裡只接 in-memory order book 與 trade/fill boundary，不接任何 DB、reservation 或 settlement runtime。
     */
    public InMemorySpotSandboxMatcher(
            InMemorySpotSandboxOrderBook orderBook,
            SpotSandboxTradeFillBoundary tradeFillBoundary
    ) {
        this.orderBook = Objects.requireNonNull(orderBook, "orderBook must not be null");
        this.tradeFillBoundary = Objects.requireNonNull(tradeFillBoundary, "tradeFillBoundary must not be null");
    }

    /**
     * 針對指定 market 執行 sandbox matching。
     *
     * 這裡只讀寫 memory 內的 order book 狀態，不會寫 DB，也不會做 settlement、ledger posting 或 balance 更新。
     */
    public synchronized SpotSandboxMatchResult match(String marketSymbol, Instant matchedAt) {
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
        Objects.requireNonNull(matchedAt, "matchedAt must not be null");

        if (!orderBook.hasMarketSymbol(marketSymbol)) {
            return SpotSandboxMatchResult.rejected(
                    SpotSandboxMatchRejectionReason.MARKET_NOT_FOUND,
                    "marketSymbol 不存在於 sandbox order book"
            );
        }

        List<SpotSandboxTradeFill> tradeFills = new ArrayList<>();

        while (true) {
            List<SpotSandboxOrderRecord> openOrders = orderBook.openOrders(marketSymbol);
            if (openOrders.isEmpty()) {
                break;
            }

            SpotSandboxMatchRejectionReason unsupportedReason = validateSupportedOrders(openOrders);
            if (unsupportedReason != null) {
                return SpotSandboxMatchResult.rejected(
                        unsupportedReason,
                        "sandbox matching 只支援 LIMIT / GTC order"
                );
            }

            var matchedPair = matchingPolicy.selectBestCrossedPair(openOrders.stream()
                    .map(InMemorySpotSandboxMatcher::toSharedCandidate)
                    .toList());
            if (matchedPair.isEmpty()) {
                break;
            }

            SandboxLimitOrderMatchPair pair = matchedPair.get();
            SpotSandboxOrderRecord bestBuy = findOrder(openOrders, pair.buyOrder().orderId());
            SpotSandboxOrderRecord bestSell = findOrder(openOrders, pair.sellOrder().orderId());
            SpotSandboxOrderRecord maker = findOrder(openOrders, pair.makerOrder().orderId());
            SpotSandboxTradeFill tradeFill = tradeFillBoundary.createTradeFill(
                    nextTradeId(matchedAt, bestBuy, bestSell, tradeFills.size()),
                    marketSymbol,
                    bestBuy.sandboxOrderId(),
                    bestSell.sandboxOrderId(),
                    bestBuy.accountId(),
                    bestSell.accountId(),
                    maker.price(),
                    pair.matchedQuantity(),
                    matchedAt,
                    SpotSandboxTradePriceRule.MAKER_PRICE
            );

            tradeFills.add(tradeFill);
            orderBook.replaceRecord(updateOrder(bestBuy, pair.matchedQuantity()));
            orderBook.replaceRecord(updateOrder(bestSell, pair.matchedQuantity()));
        }

        if (tradeFills.isEmpty()) {
            return SpotSandboxMatchResult.noMatch("BUY / SELL 未形成 crossed limit price");
        }

        return SpotSandboxMatchResult.matched(
                tradeFills,
                tradeFillBoundary.toSettlementInputs(tradeFills),
                "sandbox in-memory matching completed"
        );
    }

    private static SpotSandboxMatchRejectionReason validateSupportedOrders(List<SpotSandboxOrderRecord> orders) {
        for (SpotSandboxOrderRecord order : orders) {
            if (order.type() != SpotOrderType.LIMIT) {
                return SpotSandboxMatchRejectionReason.UNSUPPORTED_ORDER_TYPE;
            }
            if (order.timeInForce() != SpotTimeInForce.GTC) {
                return SpotSandboxMatchRejectionReason.UNSUPPORTED_TIME_IN_FORCE;
            }
        }

        return null;
    }

    private static SandboxLimitOrderCandidate toSharedCandidate(SpotSandboxOrderRecord order) {
        return new SandboxLimitOrderCandidate(
                order.sandboxOrderId(),
                order.marketSymbol(),
                order.side() == SpotOrderSide.BUY ? SandboxLimitOrderSide.BUY : SandboxLimitOrderSide.SELL,
                order.price(),
                order.remainingQuantity(),
                order.acceptedAt()
        );
    }

    private static SpotSandboxOrderRecord findOrder(List<SpotSandboxOrderRecord> orders, String sandboxOrderId) {
        // 共用 policy 只回傳同一輪輸入的 order ID；找不到代表 in-memory book 在同步區塊內違反不可變快照假設。
        return orders.stream()
                .filter(order -> order.sandboxOrderId().equals(sandboxOrderId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("shared match candidate must reference an open order"));
    }

    private static SpotSandboxOrderRecord updateOrder(SpotSandboxOrderRecord order, BigDecimal matchedQuantity) {
        BigDecimal remainingQuantity = order.remainingQuantity().subtract(matchedQuantity);
        if (remainingQuantity.compareTo(BigDecimal.ZERO) < 0) {
            remainingQuantity = BigDecimal.ZERO;
        }

        SpotSandboxOrderStatus status = remainingQuantity.compareTo(BigDecimal.ZERO) == 0
                ? SpotSandboxOrderStatus.FILLED
                : SpotSandboxOrderStatus.PARTIALLY_FILLED;

        return new SpotSandboxOrderRecord(
                order.sandboxOrderId(),
                order.requestId(),
                order.idempotencyKey(),
                order.userId(),
                order.accountId(),
                order.marketSymbol(),
                order.side(),
                order.type(),
                order.price(),
                order.quantity(),
                remainingQuantity,
                order.timeInForce(),
                status,
                order.acceptedAt()
        );
    }

    private static String nextTradeId(Instant matchedAt, SpotSandboxOrderRecord buyOrder, SpotSandboxOrderRecord sellOrder, int sequence) {
        return "sandbox-trade-" + matchedAt.toEpochMilli()
                + "-" + buyOrder.sandboxOrderId()
                + "-" + sellOrder.sandboxOrderId()
                + "-" + (sequence + 1);
    }
}
