package com.lumix.trading.core.spot.matching;

import com.lumix.trading.core.spot.orderbook.InMemorySpotSandboxOrderBook;
import com.lumix.trading.core.spot.orderbook.SpotSandboxOrderRecord;
import com.lumix.trading.core.spot.orderbook.SpotSandboxOrderStatus;
import com.lumix.trading.core.spot.orderintake.SpotOrderSide;
import com.lumix.trading.core.spot.orderintake.SpotOrderType;
import com.lumix.trading.core.spot.orderintake.SpotTimeInForce;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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

            List<SpotSandboxOrderRecord> buyOrders = sortBuys(openOrders);
            List<SpotSandboxOrderRecord> sellOrders = sortSells(openOrders);
            if (buyOrders.isEmpty() || sellOrders.isEmpty()) {
                break;
            }

            SpotSandboxOrderRecord bestBuy = buyOrders.get(0);
            SpotSandboxOrderRecord bestSell = sellOrders.get(0);

            if (bestBuy.price().compareTo(bestSell.price()) < 0) {
                break;
            }

            BigDecimal matchedQuantity = bestBuy.remainingQuantity().min(bestSell.remainingQuantity());
            SpotSandboxOrderRecord maker = comparePriority(bestBuy, bestSell) <= 0 ? bestBuy : bestSell;
            SpotSandboxTradeFill tradeFill = tradeFillBoundary.createTradeFill(
                    nextTradeId(matchedAt, bestBuy, bestSell, tradeFills.size()),
                    marketSymbol,
                    bestBuy.sandboxOrderId(),
                    bestSell.sandboxOrderId(),
                    bestBuy.accountId(),
                    bestSell.accountId(),
                    maker.price(),
                    matchedQuantity,
                    matchedAt,
                    SpotSandboxTradePriceRule.MAKER_PRICE
            );

            tradeFills.add(tradeFill);
            orderBook.replaceRecord(updateOrder(bestBuy, matchedQuantity));
            orderBook.replaceRecord(updateOrder(bestSell, matchedQuantity));
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

    private static List<SpotSandboxOrderRecord> sortBuys(List<SpotSandboxOrderRecord> orders) {
        return new ArrayList<>(orders.stream()
                .filter(order -> order.side() == SpotOrderSide.BUY && InMemorySpotSandboxOrderBook.isActiveOrder(order))
                .sorted(Comparator
                        .comparing(SpotSandboxOrderRecord::price).reversed()
                        .thenComparing(SpotSandboxOrderRecord::acceptedAt)
                        .thenComparing(SpotSandboxOrderRecord::sandboxOrderId))
                .toList());
    }

    private static List<SpotSandboxOrderRecord> sortSells(List<SpotSandboxOrderRecord> orders) {
        return new ArrayList<>(orders.stream()
                .filter(order -> order.side() == SpotOrderSide.SELL && InMemorySpotSandboxOrderBook.isActiveOrder(order))
                .sorted(Comparator
                        .comparing(SpotSandboxOrderRecord::price)
                        .thenComparing(SpotSandboxOrderRecord::acceptedAt)
                        .thenComparing(SpotSandboxOrderRecord::sandboxOrderId))
                .toList());
    }

    private static int comparePriority(SpotSandboxOrderRecord left, SpotSandboxOrderRecord right) {
        int acceptedAtCompare = left.acceptedAt().compareTo(right.acceptedAt());
        if (acceptedAtCompare != 0) {
            return acceptedAtCompare;
        }

        return left.sandboxOrderId().compareTo(right.sandboxOrderId());
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
