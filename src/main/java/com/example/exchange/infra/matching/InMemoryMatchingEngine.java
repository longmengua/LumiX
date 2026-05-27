/*
 * 檔案用途：撮合基礎設施，提供目前的 in-memory matching engine 實作。
 */
package com.example.exchange.infra.matching;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.MatchingEngineSnapshot;
import com.example.exchange.domain.model.dto.MatchingResult;
import com.example.exchange.domain.model.dto.TopOfBook;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.model.enums.TimeInForce;
import com.example.exchange.domain.service.MatchingEngine;
import com.example.exchange.domain.service.OrderBook;
import com.example.exchange.domain.service.OrderBookSnapshot;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 記憶體撮合引擎。
 *
 * P1 版本把每個 symbol 收斂到單一 sequencer thread，所有 submit/cancel/snapshot
 * 都在該 symbol 的執行緒中序列化處理，避免多請求同時改同一本 book。
 */
@Component
public class InMemoryMatchingEngine implements MatchingEngine {

    private final Map<String, SymbolRuntime> runtimes = new ConcurrentHashMap<>();

    @Override
    public MatchingResult submit(Order order) {
        SymbolRuntime runtime = runtime(order.getSymbol().code());
        return runtime.call(() -> submitOnSequencer(runtime, order));
    }

    @Override
    public boolean cancelOrder(Order order) {
        SymbolRuntime runtime = runtimes.get(order.getSymbol().code());
        if (runtime == null) return false;
        return runtime.call(() -> runtime.book.cancel(order));
    }

    @Override
    public boolean amendOrder(Order order, BigDecimal newPrice, BigDecimal newQty) {
        SymbolRuntime runtime = runtimes.get(order.getSymbol().code());
        if (runtime == null) return false;
        return runtime.call(() -> runtime.book.amend(order, newPrice, newQty));
    }

    @Override
    public OrderBookSnapshot snapshot(String symbolCode, int depth) {
        SymbolRuntime runtime = runtimes.get(normalize(symbolCode));
        if (runtime == null) return new OrderBookSnapshot(List.of(), List.of());
        return runtime.call(() -> runtime.book.snapshot(Math.max(1, depth)));
    }

    @Override
    public Optional<TopOfBook> top(String symbolCode) {
        SymbolRuntime runtime = runtimes.get(normalize(symbolCode));
        if (runtime == null) return Optional.empty();
        return runtime.call(() -> {
            BigDecimal bestBid = runtime.book.peekBestBid() == null ? null : runtime.book.peekBestBid().getPrice();
            BigDecimal bestAsk = runtime.book.peekBestAsk() == null ? null : runtime.book.peekBestAsk().getPrice();
            if (bestBid == null && bestAsk == null) return Optional.empty();
            return Optional.of(TopOfBook.builder().bestBid(bestBid).bestAsk(bestAsk).build());
        });
    }

    @Override
    public MatchingEngineSnapshot exportSnapshot(String symbolCode) {
        String symbol = normalize(symbolCode);
        SymbolRuntime runtime = runtimes.get(symbol);
        if (runtime == null) {
            return new MatchingEngineSnapshot(symbol, 0L, List.of(), List.of(), Instant.now());
        }
        return runtime.call(() -> new MatchingEngineSnapshot(
                symbol,
                runtime.matchSeq.get(),
                List.copyOf(runtime.book.restingOrders(OrderSide.BUY)),
                List.copyOf(runtime.book.restingOrders(OrderSide.SELL)),
                Instant.now()
        ));
    }

    @Override
    public void restoreSnapshot(MatchingEngineSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
        SymbolRuntime runtime = runtime(snapshot.symbolCode());
        runtime.call(() -> {
            runtime.book.clear();
            runtime.matchSeq.set(Math.max(0L, snapshot.matchSequence()));
            restoreOrders(runtime.book, snapshot.bids());
            restoreOrders(runtime.book, snapshot.asks());
            return null;
        });
    }

    @PreDestroy
    public void shutdown() {
        runtimes.values().forEach(runtime -> runtime.executor.shutdownNow());
    }

    private MatchingResult submitOnSequencer(SymbolRuntime runtime, Order order) {
        OrderBook orderBook = runtime.book;
        List<TradeExecuted> trades = new ArrayList<>();
        LinkedHashSet<Order> affectedOrders = new LinkedHashSet<>();
        affectedOrders.add(order);

        TimeInForce tif = order.getTimeInForce() == null ? TimeInForce.GTC : order.getTimeInForce();

        if (order.isPostOnly() && canMatch(order, orderBook)) {
            order.reject("POST_ONLY_WOULD_TAKE");
            return result(trades, affectedOrders);
        }

        if (tif == TimeInForce.FOK) {
            BigDecimal matchable = orderBook.matchableQty(order);
            if (matchable.compareTo(order.getQty()) < 0) {
                order.expire("FOK_NOT_FULLY_FILLABLE");
                return result(trades, affectedOrders);
            }
        }

        if (order.getSide() == OrderSide.BUY) {
            matchBuy(runtime, order, trades, affectedOrders);
        } else {
            matchSell(runtime, order, trades, affectedOrders);
        }

        if (order.getQty().signum() > 0 && order.getStatus() != Order.Status.REJECTED) {
            switch (tif) {
                case IOC -> order.expire(iocExpireReason(order));
                case FOK -> order.expire("FOK_NOT_FULLY_FILLABLE");
                case GTC -> handleGtcRemainder(orderBook, order);
            }
        }

        return result(trades, affectedOrders);
    }

    private void matchBuy(
            SymbolRuntime runtime,
            Order order,
            List<TradeExecuted> trades,
            LinkedHashSet<Order> affectedOrders
    ) {
        OrderBook orderBook = runtime.book;
        while (canMatchBuy(order, orderBook)) {
            Order bestAsk = orderBook.peekBestAsk();
            if (bestAsk == null) return;
            if (isSelfMatch(order, bestAsk)) {
                stopForSelfMatch(order);
                return;
            }
            affectedOrders.add(bestAsk);

            BigDecimal execQty = order.getQty().min(bestAsk.getQty());
            BigDecimal execPx = bestAsk.getPrice();
            String matchId = nextMatchId(runtime, order.getSymbol().code());
            Instant now = Instant.now();

            trades.add(trade(order, bestAsk, execQty, execPx, matchId, false, now));
            trades.add(trade(bestAsk, order, execQty.negate(), execPx, matchId, true, now));

            order.fill(execQty, execPx);
            bestAsk.fill(execQty, execPx);
            orderBook.removeFilled(bestAsk);
        }
    }

    private void matchSell(
            SymbolRuntime runtime,
            Order order,
            List<TradeExecuted> trades,
            LinkedHashSet<Order> affectedOrders
    ) {
        OrderBook orderBook = runtime.book;
        while (canMatchSell(order, orderBook)) {
            Order bestBid = orderBook.peekBestBid();
            if (bestBid == null) return;
            if (isSelfMatch(order, bestBid)) {
                stopForSelfMatch(order);
                return;
            }
            affectedOrders.add(bestBid);

            BigDecimal execQty = order.getQty().min(bestBid.getQty());
            BigDecimal execPx = bestBid.getPrice();
            String matchId = nextMatchId(runtime, order.getSymbol().code());
            Instant now = Instant.now();

            trades.add(trade(order, bestBid, execQty.negate(), execPx, matchId, false, now));
            trades.add(trade(bestBid, order, execQty, execPx, matchId, true, now));

            order.fill(execQty, execPx);
            bestBid.fill(execQty, execPx);
            orderBook.removeFilled(bestBid);
        }
    }

    private void handleGtcRemainder(OrderBook orderBook, Order order) {
        if (order.getType() == OrderType.MARKET) {
            order.expire("MARKET_LIQUIDITY_INSUFFICIENT");
            return;
        }
        orderBook.add(order);
    }

    private boolean canMatch(Order order, OrderBook orderBook) {
        return order.getSide() == OrderSide.BUY
                ? canMatchBuy(order, orderBook)
                : canMatchSell(order, orderBook);
    }

    private boolean canMatchBuy(Order order, OrderBook orderBook) {
        Order bestAsk = orderBook.peekBestAsk();
        if (bestAsk == null || order.getQty().signum() <= 0) return false;
        if (order.getType() == OrderType.MARKET) return true;
        return order.getPrice() != null && order.getPrice().compareTo(bestAsk.getPrice()) >= 0;
    }

    private boolean canMatchSell(Order order, OrderBook orderBook) {
        Order bestBid = orderBook.peekBestBid();
        if (bestBid == null || order.getQty().signum() <= 0) return false;
        if (order.getType() == OrderType.MARKET) return true;
        return order.getPrice() != null && order.getPrice().compareTo(bestBid.getPrice()) <= 0;
    }

    private static TradeExecuted trade(
            Order owner,
            Order counterparty,
            BigDecimal signedQty,
            BigDecimal price,
            String matchId,
            boolean maker,
            Instant ts
    ) {
        return new TradeExecuted(
                owner.getUid(),
                owner.getSymbol(),
                signedQty,
                price,
                0L,
                ts,
                owner.getId(),
                counterparty.getId(),
                matchId,
                maker
        );
    }

    private static boolean isSelfMatch(Order incoming, Order resting) {
        return incoming.getUid() == resting.getUid();
    }

    private static void stopForSelfMatch(Order order) {
        if (order.getExecutedQty() != null && order.getExecutedQty().signum() > 0) {
            order.expire("SELF_MATCH_REMAINDER_EXPIRED");
        } else {
            order.reject("SELF_MATCH_PREVENTED");
        }
    }

    private static String iocExpireReason(Order order) {
        if (order.getExecutedQty() != null && order.getExecutedQty().signum() > 0) {
            return "IOC_REMAINDER_EXPIRED";
        }
        return "IOC_NOT_FILLED";
    }

    private static MatchingResult result(List<TradeExecuted> trades, LinkedHashSet<Order> affectedOrders) {
        return MatchingResult.builder()
                .trades(trades)
                .affectedOrders(new ArrayList<>(affectedOrders))
                .build();
    }

    private SymbolRuntime runtime(String symbolCode) {
        String symbol = normalize(symbolCode);
        return runtimes.computeIfAbsent(symbol, SymbolRuntime::new);
    }

    private static String nextMatchId(SymbolRuntime runtime, String symbol) {
        return normalize(symbol) + "-" + runtime.matchSeq.incrementAndGet();
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }

    private static void restoreOrders(OrderBook book, List<Order> orders) {
        if (orders == null) return;
        for (Order order : orders) {
            if (order == null
                    || order.getId() == null
                    || order.getPrice() == null
                    || order.getQty() == null
                    || order.getQty().signum() <= 0) {
                continue;
            }
            book.add(order);
        }
    }

    private static final class SymbolRuntime {
        private final OrderBook book = new OrderBook();
        private final AtomicLong matchSeq = new AtomicLong();
        private final ExecutorService executor;

        private SymbolRuntime(String symbol) {
            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setName("matching-" + symbol);
                thread.setDaemon(true);
                return thread;
            });
        }

        private <T> T call(Callable<T> callable) {
            try {
                Future<T> future = executor.submit(callable);
                return future.get();
            } catch (Exception ex) {
                if (ex.getCause() instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("matching sequencer failed", ex);
            }
        }
    }
}
