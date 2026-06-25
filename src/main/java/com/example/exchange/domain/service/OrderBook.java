/*
 * 檔案用途：領域服務，封裝撮合、風控、Polymarket 同步與交易規則。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PriceLevel;
import com.example.exchange.domain.model.dto.Order;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 單一交易對訂單簿。
 *
 * 結構：
 * - bids：價格由低到高存放，最佳買價取 lastEntry。
 * - asks：價格由低到高存放，最佳賣價取 firstEntry。
 * - 同一價格使用 LinkedHashSet 保留 FIFO 時間優先。
 * - orderIndex 提供穩定 orderId 查找路徑。
 */
public class OrderBook {

    private final NavigableMap<BigDecimal, LinkedHashSet<Order>> bids = new TreeMap<>();
    private final NavigableMap<BigDecimal, LinkedHashSet<Order>> asks = new TreeMap<>();
    private final Map<UUID, Order> orderIndex = new HashMap<>();

    public Order peekBestBid() {
        return firstOrder(bids.lastEntry());
    }

    public Order peekBestAsk() {
        return firstOrder(asks.firstEntry());
    }

    public Order pollBestBid() {
        Order order = peekBestBid();
        if (order != null) remove(order);
        return order;
    }

    public Order pollBestAsk() {
        Order order = peekBestAsk();
        if (order != null) remove(order);
        return order;
    }

    public void add(Order order) {
        if (order == null || order.getId() == null) return;
        if (order.getPrice() == null) {
            throw new IllegalArgumentException("only priced orders can enter order book");
        }
        cancel(order);
        NavigableMap<BigDecimal, LinkedHashSet<Order>> sideBook = sideBook(order.getSide());
        sideBook.computeIfAbsent(order.getPrice(), ignored -> new LinkedHashSet<>()).add(order);
        orderIndex.put(order.getId(), order);
    }

    public boolean cancel(Order order) {
        if (order == null || order.getId() == null) return false;
        Order indexed = orderIndex.get(order.getId());
        if (indexed == null) return false;
        remove(indexed);
        return true;
    }

    public boolean contains(UUID orderId) {
        return orderIndex.containsKey(orderId);
    }

    public Order find(UUID orderId) {
        return orderIndex.get(orderId);
    }

    public boolean amend(Order order, BigDecimal newPrice, BigDecimal newQty) {
        if (order == null || order.getId() == null) return false;
        Order indexed = orderIndex.get(order.getId());
        if (indexed == null) return false;
        remove(indexed);
        if (newPrice != null) indexed.setPrice(newPrice);
        if (newQty != null) indexed.setQty(newQty);
        add(indexed);
        return true;
    }

    public void clear() {
        bids.clear();
        asks.clear();
        orderIndex.clear();
    }

    public List<Order> restingOrders(OrderSide side) {
        NavigableMap<BigDecimal, LinkedHashSet<Order>> book = side == OrderSide.BUY
                ? bids.descendingMap()
                : asks;
        List<Order> orders = new ArrayList<>();
        for (LinkedHashSet<Order> level : book.values()) {
            orders.addAll(level);
        }
        return orders;
    }

    public void removeFilled(Order order) {
        if (order != null && order.getQty() != null && order.getQty().signum() <= 0) {
            remove(order);
        }
    }

    public BigDecimal matchableQty(Order incoming) {
        if (incoming == null || incoming.getQty() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal remaining = incoming.getQty();
        BigDecimal matched = BigDecimal.ZERO;

        Iterable<Map.Entry<BigDecimal, LinkedHashSet<Order>>> levels = incoming.getSide() == OrderSide.BUY
                ? asks.entrySet()
                : bids.descendingMap().entrySet();

        for (Map.Entry<BigDecimal, LinkedHashSet<Order>> level : levels) {
            if (remaining.signum() <= 0) break;
            if (!priceCrosses(incoming, level.getKey())) break;

            for (Order resting : level.getValue()) {
                if (remaining.signum() <= 0) break;
                if (resting.getQty() == null || resting.getQty().signum() <= 0) continue;
                BigDecimal execQty = remaining.min(resting.getQty());
                matched = matched.add(execQty);
                remaining = remaining.subtract(execQty);
            }
        }

        return matched;
    }

    public OrderBookSnapshot snapshot(int depth) {
        int safeDepth = Math.max(1, depth);
        return new OrderBookSnapshot(
                aggregate(bids.descendingMap(), safeDepth),
                aggregate(asks, safeDepth)
        );
    }

    private void remove(Order order) {
        NavigableMap<BigDecimal, LinkedHashSet<Order>> sideBook = sideBook(order.getSide());
        LinkedHashSet<Order> level = sideBook.get(order.getPrice());
        if (level != null) {
            level.remove(order);
            if (level.isEmpty()) {
                sideBook.remove(order.getPrice());
            }
        }
        orderIndex.remove(order.getId());
    }

    private NavigableMap<BigDecimal, LinkedHashSet<Order>> sideBook(OrderSide side) {
        return side == OrderSide.BUY ? bids : asks;
    }

    private static Order firstOrder(Map.Entry<BigDecimal, LinkedHashSet<Order>> level) {
        if (level == null || level.getValue().isEmpty()) return null;
        return level.getValue().iterator().next();
    }

    private static boolean priceCrosses(Order incoming, BigDecimal restingPrice) {
        if (incoming.getType() == OrderType.MARKET) return true;
        if (incoming.getPrice() == null) return false;
        return incoming.getSide() == OrderSide.BUY
                ? incoming.getPrice().compareTo(restingPrice) >= 0
                : incoming.getPrice().compareTo(restingPrice) <= 0;
    }

    private static List<PriceLevel> aggregate(
            NavigableMap<BigDecimal, LinkedHashSet<Order>> book,
            int depth
    ) {
        List<PriceLevel> levels = new ArrayList<>();
        for (Map.Entry<BigDecimal, LinkedHashSet<Order>> entry : book.entrySet()) {
            BigDecimal qty = entry.getValue().stream()
                    .map(Order::getQty)
                    .filter(v -> v != null && v.signum() > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (qty.signum() <= 0) continue;
            levels.add(new PriceLevel(entry.getKey(), qty));
            if (levels.size() >= depth) break;
        }
        return levels;
    }
}
