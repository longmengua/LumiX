package com.example.exchange.infra.matching;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.Order;
import com.example.exchange.domain.model.OrderSide;
import com.example.exchange.domain.model.OrderType;
import com.example.exchange.domain.service.MatchingEngine;
import com.example.exchange.domain.service.OrderBook;
import com.example.exchange.domain.service.OrderBookSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory 撮合引擎（多交易對）
 *
 * 功能重點：
 * 1) 先以「價格撮合」處理傳入訂單（taker）
 * 2) 若為 MARKET 且未完全成交 → 轉為 LIMIT 掛簿（maker）(Market-To-Limit)
 *    - 掛簿價優先取「最後成交價」，否則取該側合理的 Top-of-Book 價位
 */
@Component
public class InMemoryMatchingEngine implements MatchingEngine {

    /** key = symbolCode（例：BTCUSDT） */
    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();

    private OrderBook book(String symbolCode) {
        return books.computeIfAbsent(symbolCode, k -> new OrderBook());
    }

    @Override
    public List<TradeExecuted> submitOrder(Order order) {
        String sym = order.getSymbol().code();
        OrderBook ob = book(sym);
        List<TradeExecuted> trades = new ArrayList<>();

        synchronized (ob) {
            BigDecimal lastExecPrice = null; // 紀錄最後一筆成交價（MTL 轉掛價會用到）

            if (order.getSide() == OrderSide.BUY) {
                // -----------------------------
                // BUY：買單與賣單簿撮合（買價 >= 最佳賣價）
                // -----------------------------
                while (ob.peekBestAsk() != null
                        && order.getQty().signum() > 0
                        && order.getPrice() != null
                        && order.getPrice().compareTo(ob.peekBestAsk().getPrice()) >= 0) {

                    Order bestAsk = ob.pollBestAsk();
                    BigDecimal execQty = order.getQty().min(bestAsk.getQty());
                    BigDecimal execPx  = bestAsk.getPrice();

                    // 雙邊成交事件（買方 +qty、賣方 -qty）
                    trades.add(new TradeExecuted(order.getUid(),     order.getSymbol(),    execQty,          execPx, 0L, Instant.now()));
                    trades.add(new TradeExecuted(bestAsk.getUid(),   bestAsk.getSymbol(),  execQty.negate(), execPx, 0L, Instant.now()));

                    lastExecPrice = execPx; // 記錄最後成交價（供 MTL 用）

                    // 更新剩餘
                    order.fill(execQty);
                    bestAsk.fill(execQty);
                    if (bestAsk.getQty().signum() > 0) ob.add(bestAsk); // 部分成交 → 放回賣簿
                }

                // ---- 殘量處理 ----
                if (order.getQty().signum() > 0) {
                    if (order.getType() == OrderType.MARKET) {
                        // ✅ MTL：市價單殘量 → 轉掛 LIMIT（maker）
                        BigDecimal postPx = choosePostPriceForBuy(ob, order, lastExecPrice);
                        order.setType(OrderType.LIMIT);
                        order.setPrice(postPx);
                        ob.add(order);
                    } else {
                        // LIMIT：吃不完就進簿
                        ob.add(order);
                    }
                }

            } else {
                // -----------------------------
                // SELL：賣單與買單簿撮合（賣價 <= 最佳買價）
                // -----------------------------
                while (ob.peekBestBid() != null
                        && order.getQty().signum() > 0
                        && order.getPrice() != null
                        && order.getPrice().compareTo(ob.peekBestBid().getPrice()) <= 0) {

                    Order bestBid = ob.pollBestBid();
                    BigDecimal execQty = order.getQty().min(bestBid.getQty());
                    BigDecimal execPx  = bestBid.getPrice();

                    trades.add(new TradeExecuted(order.getUid(),    order.getSymbol(),   execQty.negate(), execPx, 0L, Instant.now()));
                    trades.add(new TradeExecuted(bestBid.getUid(),  bestBid.getSymbol(), execQty,          execPx, 0L, Instant.now()));

                    lastExecPrice = execPx;

                    order.fill(execQty);
                    bestBid.fill(execQty);
                    if (bestBid.getQty().signum() > 0) ob.add(bestBid);
                }

                // ---- 殘量處理 ----
                if (order.getQty().signum() > 0) {
                    if (order.getType() == OrderType.MARKET) {
                        // ✅ MTL：市價單殘量 → 轉掛 LIMIT（maker）
                        BigDecimal postPx = choosePostPriceForSell(ob, order, lastExecPrice);
                        order.setType(OrderType.LIMIT);
                        order.setPrice(postPx);
                        ob.add(order);
                    } else {
                        // LIMIT：吃不完就進簿
                        ob.add(order);
                    }
                }
            }
        }

        return trades;
    }

    /**
     * 買單 MTL 的掛簿價格選擇邏輯
     * 優先順序：
     * 1) lastExecPrice（若本次有成交）
     * 2) 現在的 Top-of-Book 合理價位（先看 bestBid，沒有就看 bestAsk）
     * 3) 否則保留原價（極端價也無妨，因為當下沒有對手方，不會立即成交）
     */
    private BigDecimal choosePostPriceForBuy(OrderBook ob, Order order, BigDecimal lastExecPrice) {
        if (lastExecPrice != null) return lastExecPrice;
        var bestBid = ob.peekBestBid();
        if (bestBid != null) return bestBid.getPrice();
        var bestAsk = ob.peekBestAsk();
        if (bestAsk != null) return bestAsk.getPrice();
        return order.getPrice(); // 退路：保留傳入價格（對市價單通常是極端價，但此時簿上無對手方，不會立刻成交）
    }

    /**
     * 賣單 MTL 的掛簿價格選擇邏輯
     * 優先順序：
     * 1) lastExecPrice（若本次有成交）
     * 2) 現在的 Top-of-Book 合理價位（先看 bestAsk，沒有就看 bestBid）
     * 3) 否則保留原價
     */
    private BigDecimal choosePostPriceForSell(OrderBook ob, Order order, BigDecimal lastExecPrice) {
        if (lastExecPrice != null) return lastExecPrice;
        var bestAsk = ob.peekBestAsk();
        if (bestAsk != null) return bestAsk.getPrice();
        var bestBid = ob.peekBestBid();
        if (bestBid != null) return bestBid.getPrice();
        return order.getPrice();
    }

    @Override
    public boolean cancelOrder(Order order) {
        OrderBook ob = books.get(order.getSymbol().code());
        if (ob == null) return false;
        synchronized (ob) {
            return ob.cancel(order);
        }
    }

    @Override
    public OrderBookSnapshot snapshot(String symbolCode, int depth) {
        OrderBook ob = books.get(symbolCode);
        if (ob == null) return new OrderBookSnapshot(List.of(), List.of());
        synchronized (ob) {
            return ob.snapshot(Math.max(1, depth));
        }
    }

    @Override
    public Optional<TopOfBook> top(String symbolCode) {
        OrderBook ob = books.get(symbolCode);
        if (ob == null) return Optional.empty();
        synchronized (ob) {
            var bid = (ob.peekBestBid() != null) ? ob.peekBestBid().getPrice() : null;
            var ask = (ob.peekBestAsk() != null) ? ob.peekBestAsk().getPrice() : null;
            return Optional.of(new TopOfBook(bid, ask));
        }
    }
}
