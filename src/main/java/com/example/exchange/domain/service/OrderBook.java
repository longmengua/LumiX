package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PriceLevel;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * OrderBook（單一交易對訂單簿）
 * -------------------------------------------------
 *
 * 角色定位：
 * - 此類別負責維護某一個交易對（例如 BTCUSDT）的買賣掛單資料。
 * - 撮合引擎會透過它取得最佳買價/賣價、取出對手單、加入新掛單、取消掛單，
 *   以及輸出深度快照。
 *
 * 設計說明：
 * 1) bids（買簿）：
 *    - 使用 PriorityQueue
 *    - 價格由高到低排序（價格高者優先）
 *
 * 2) asks（賣簿）：
 *    - 使用 PriorityQueue
 *    - 價格由低到高排序（價格低者優先）
 *
 * 目前限制：
 * - 僅實作「價格優先」
 * - 尚未加入「時間優先（FIFO）」規則
 * - 因此目前仍屬於簡化版撮合簿，不是完整交易所級別實作
 *
 * 後續可擴充：
 * - 價格優先 + 時間優先
 * - 每一個價位對應 FIFO queue
 * - 支援 IOC / FOK 的更完整預估
 * - 支援 reduce-only / post-only 等訂單限制
 */
public class OrderBook {

    /**
     * 買單簿（bids）
     * - 價格由高到低排序
     * - 最佳買價在 queue 頂端
     */
    private final PriorityQueue<Order> bids =
            new PriorityQueue<>(Comparator.comparing(Order::getPrice).reversed());

    /**
     * 賣單簿（asks）
     * - 價格由低到高排序
     * - 最佳賣價在 queue 頂端
     */
    private final PriorityQueue<Order> asks =
            new PriorityQueue<>(Comparator.comparing(Order::getPrice));

    /**
     * 查看最佳買單（不移除）
     *
     * @return 最佳買單；若買簿為空則回傳 null
     */
    public Order peekBestBid() {
        return bids.peek();
    }

    /**
     * 查看最佳賣單（不移除）
     *
     * @return 最佳賣單；若賣簿為空則回傳 null
     */
    public Order peekBestAsk() {
        return asks.peek();
    }

    /**
     * 取出最佳買單（會移除）
     *
     * @return 最佳買單；若買簿為空則回傳 null
     */
    public Order pollBestBid() {
        return bids.poll();
    }

    /**
     * 取出最佳賣單（會移除）
     *
     * @return 最佳賣單；若賣簿為空則回傳 null
     */
    public Order pollBestAsk() {
        return asks.poll();
    }

    /**
     * 將訂單加入訂單簿
     * -------------------------------------------------
     * - BUY 進 bids
     * - SELL 進 asks
     *
     * 注意：
     * - 一般只有 LIMIT / 可掛簿的訂單才應進入此方法
     * - 正統 MARKET 單不應掛進 order book
     *
     * @param order 欲加入掛簿的訂單
     */
    public void add(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            bids.add(order);
        } else {
            asks.add(order);
        }
    }

    /**
     * 從訂單簿中移除指定訂單
     * -------------------------------------------------
     * 注意：
     * - 這裡依賴 PriorityQueue.remove(object)
     * - 是否能正確移除，取決於 Order 的 equals/hashCode 實作
     *
     * @param order 欲取消的訂單
     * @return true 表示成功移除；false 表示未找到
     */
    public boolean cancel(Order order) {
        return (order.getSide() == OrderSide.BUY)
                ? bids.remove(order)
                : asks.remove(order);
    }

    /**
     * 預估此訂單在當前訂單簿中最多可成交多少數量
     * -------------------------------------------------
     * 用途：
     * - 可作為 FOK（Fill Or Kill）的判斷依據
     * - 也可作為撮合前的可成交量估算
     *
     * 規則：
     * - LIMIT BUY：
     *   只累加 asks 中「價格 <= 買價」的掛單量
     *
     * - LIMIT SELL：
     *   只累加 bids 中「價格 >= 賣價」的掛單量
     *
     * - MARKET BUY：
     *   不看價格限制，直接累加 asks 的量，直到滿足委託數量或賣簿為空
     *
     * - MARKET SELL：
     *   不看價格限制，直接累加 bids 的量，直到滿足委託數量或買簿為空
     *
     * 注意：
     * - 此方法不會修改原始訂單簿
     * - 此方法目前僅依價格優先做估算，尚未考慮時間優先
     *
     * @param incoming 傳入的新訂單
     * @return 在目前簿況下理論上最多可成交的數量
     */
    public BigDecimal matchableQty(Order incoming) {
        if (incoming == null || incoming.getQty() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal remaining = incoming.getQty();
        BigDecimal matched = BigDecimal.ZERO;

        if (incoming.getSide() == OrderSide.BUY) {
            // 買單看賣簿：價格由低到高
            List<Order> sortedAsks = new ArrayList<>(asks);
            sortedAsks.sort(Comparator.comparing(Order::getPrice));

            for (Order ask : sortedAsks) {
                if (remaining.signum() <= 0) {
                    break;
                }
                if (ask.getQty() == null || ask.getQty().signum() <= 0) {
                    continue;
                }

                // LIMIT BUY 需滿足價格條件；MARKET BUY 不需要
                if (incoming.getType() == OrderType.LIMIT) {
                    if (incoming.getPrice() == null) {
                        return BigDecimal.ZERO;
                    }
                    if (incoming.getPrice().compareTo(ask.getPrice()) < 0) {
                        break;
                    }
                }

                BigDecimal execQty = remaining.min(ask.getQty());
                matched = matched.add(execQty);
                remaining = remaining.subtract(execQty);
            }
        } else {
            // 賣單看買簿：價格由高到低
            List<Order> sortedBids = new ArrayList<>(bids);
            sortedBids.sort(Comparator.comparing(Order::getPrice).reversed());

            for (Order bid : sortedBids) {
                if (remaining.signum() <= 0) {
                    break;
                }
                if (bid.getQty() == null || bid.getQty().signum() <= 0) {
                    continue;
                }

                // LIMIT SELL 需滿足價格條件；MARKET SELL 不需要
                if (incoming.getType() == OrderType.LIMIT) {
                    if (incoming.getPrice() == null) {
                        return BigDecimal.ZERO;
                    }
                    if (incoming.getPrice().compareTo(bid.getPrice()) > 0) {
                        break;
                    }
                }

                BigDecimal execQty = remaining.min(bid.getQty());
                matched = matched.add(execQty);
                remaining = remaining.subtract(execQty);
            }
        }

        return matched;
    }

    /**
     * 取得訂單簿快照（按價格聚合）
     * -------------------------------------------------
     * 功能：
     * - 將相同價格的多張訂單聚合成一個價位
     * - bids 由高到低輸出
     * - asks 由低到高輸出
     *
     * @param depth 最多回傳幾檔深度（至少為 1）
     * @return 訂單簿快照
     */
    public OrderBookSnapshot snapshot(int depth) {
        int safeDepth = Math.max(1, depth);

        List<PriceLevel> bidLevels = aggregateByPrice(bids, true, safeDepth);
        List<PriceLevel> askLevels = aggregateByPrice(asks, false, safeDepth);

        return new OrderBookSnapshot(bidLevels, askLevels);
    }

    /**
     * 將訂單集合依價格聚合成價位列表
     * -------------------------------------------------
     * 設計說明：
     * - PriorityQueue 的 iterator 不保證排序
     * - 因此先複製為 List 再排序
     * - 再用 LinkedHashMap 依價格做數量聚合，以保留排序後順序
     *
     * @param orders 原始訂單集合
     * @param desc   true = 由高到低，false = 由低到高
     * @param depth  最多保留幾個價位
     * @return 聚合後的價位列表
     */
    private static List<PriceLevel> aggregateByPrice(Collection<Order> orders, boolean desc, int depth) {
        List<Order> sorted = new ArrayList<>(orders);

        sorted.sort(desc
                ? Comparator.comparing(Order::getPrice).reversed()
                : Comparator.comparing(Order::getPrice));

        Map<BigDecimal, BigDecimal> aggregated = new LinkedHashMap<>();

        for (Order order : sorted) {
            if (order.getPrice() == null || order.getQty() == null) {
                continue;
            }

            aggregated.merge(order.getPrice(), order.getQty(), BigDecimal::add);

            // 注意：
            // 這裡是「價位數」達到 depth 就停止，不是訂單數達到 depth
            if (aggregated.size() >= depth) {
                break;
            }
        }

        return aggregated.entrySet().stream()
                .map(entry -> new PriceLevel(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}