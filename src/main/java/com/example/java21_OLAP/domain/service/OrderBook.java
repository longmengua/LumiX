package com.example.java21_OLAP.domain.service;

import com.example.java21_OLAP.domain.model.Order;
import com.example.java21_OLAP.domain.model.OrderSide;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 單一交易對訂單簿（極簡）
 * - 僅依價格排序；未加入時間優先
 */
public class OrderBook {

    private final PriorityQueue<Order> bids =
            new PriorityQueue<>(Comparator.comparing(Order::getPrice).reversed());
    private final PriorityQueue<Order> asks =
            new PriorityQueue<>(Comparator.comparing(Order::getPrice));

    public Order peekBestBid() { return bids.peek(); }
    public Order peekBestAsk() { return asks.peek(); }
    public Order pollBestBid() { return bids.poll(); }
    public Order pollBestAsk() { return asks.poll(); }

    public void add(Order order) {
        if (order.getSide() == OrderSide.BUY) bids.add(order);
        else                                   asks.add(order);
    }

    public boolean cancel(Order order) {
        return (order.getSide() == OrderSide.BUY) ? bids.remove(order) : asks.remove(order);
    }

    /**
     * 取得簿的快照（深度聚合）
     * - 會把相同 price 的訂單做 qty 聚合
     * - bids 由高到低、asks 由低到高
     */
    public OrderBookSnapshot snapshot(int depth) {
        List<PriceLevel> bidLevels = aggregateByPrice(bids, /*desc=*/true, depth);
        List<PriceLevel> askLevels = aggregateByPrice(asks, /*desc=*/false, depth);
        return new OrderBookSnapshot(bidLevels, askLevels);
    }

    private static List<PriceLevel> aggregateByPrice(Collection<Order> orders, boolean desc, int depth) {
        // PriorityQueue 的 iterator 非排序，所以先複製成 List 再排序
        List<Order> sorted = new ArrayList<>(orders);
        sorted.sort(desc
                ? Comparator.comparing(Order::getPrice).reversed()
                : Comparator.comparing(Order::getPrice));

        // 依價格聚合數量（使用 LinkedHashMap 維持順序）
        Map<BigDecimal, BigDecimal> agg = new LinkedHashMap<>();
        for (Order o : sorted) {
            agg.merge(o.getPrice(), o.getQty(), BigDecimal::add);
            if (agg.size() >= depth) break; // 只取到指定深度
        }

        return agg.entrySet().stream()
                .map(e -> new PriceLevel(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
