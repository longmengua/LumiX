package com.example.exchange.infra.matching;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.MatchingResult;
import com.example.exchange.domain.model.Order;
import com.example.exchange.domain.model.OrderSide;
import com.example.exchange.domain.model.OrderType;
import com.example.exchange.domain.model.TopOfBook;
import com.example.exchange.domain.service.MatchingEngine;
import com.example.exchange.domain.service.OrderBook;
import com.example.exchange.domain.service.OrderBookSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InMemoryMatchingEngine（記憶體版撮合引擎）
 * -------------------------------------------------
 *
 * 角色定位：
 * - 此類別是 MatchingEngine 的基礎實作，負責在記憶體中維護多個交易對的訂單簿，
 *   並對新訂單執行撮合。
 * - 每個 symbol（例如 BTCUSDT）對應一個獨立的 OrderBook。
 *
 * 設計說明：
 * 1) 使用 ConcurrentHashMap 管理多交易對的 order book
 * 2) 對單一 OrderBook 採 synchronized，避免同一交易對同時撮合造成資料競態
 * 3) 新單預設視為 taker，簿中的掛單視為 maker
 * 4) 支援：
 *    - LIMIT：能成交的先成交，吃不完則續掛簿
 *    - MARKET：能成交的先成交，吃不完則轉為 LIMIT 掛簿（簡化版 MTL）
 *
 * 注意事項：
 * - 目前是 Demo 級別實作，不是生產級撮合核心
 * - 尚未完整實作：
 *   - 價格/數量精度量化
 *   - 自成交防護（SMP）
 *   - IOC / FOK / POST_ONLY / REDUCE_ONLY
 *   - maker/taker fee
 *   - 單簿序列器 / Disruptor / 高效能無鎖模型
 *
 * 相容性：
 * - submitOrder(Order)：舊版 API，只回傳成交事件列表
 * - submitOrderV2(Order)：新版 API，回傳 trades + affectedOrders
 */
@Component
public class InMemoryMatchingEngine implements MatchingEngine {

    /**
     * 多交易對的訂單簿容器
     * - key   = symbol code（例如 BTCUSDT）
     * - value = 該交易對的 OrderBook
     */
    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();

    /**
     * 取得指定交易對的 OrderBook
     * - 若不存在則自動建立
     *
     * @param symbolCode 交易對代碼
     * @return 該交易對對應的訂單簿
     */
    private OrderBook book(String symbolCode) {
        return books.computeIfAbsent(symbolCode, k -> new OrderBook());
    }

    // -------------------------------------------------
    // 相容舊版 API
    // -------------------------------------------------

    /**
     * 舊版下單 API
     * - 僅需要成交事件列表
     * - 內部委派給 submitOrderV2()，再取出 trades
     *
     * @param order 新訂單
     * @return 成交事件列表
     */
    @Override
    public List<TradeExecuted> submitOrder(Order order) {
        return submitOrderV2(order).getTrades();
    }

    // -------------------------------------------------
    // 新版 API
    // -------------------------------------------------

    /**
     * 新版下單 API
     * -------------------------------------------------
     * 功能：
     * - 對傳入的新訂單做撮合
     * - 回傳本次撮合產生的成交事件（trades）
     * - 回傳所有受影響的訂單（affectedOrders）
     *
     * 撮合規則（簡化版）：
     * - BUY 單會去吃賣簿 asks
     * - SELL 單會去吃買簿 bids
     * - 若為 LIMIT 且有剩餘量，續掛簿
     * - 若為 MARKET 且有剩餘量，轉為 LIMIT 掛簿（MTL）
     *
     * @param order 新訂單
     * @return 撮合結果（trades + affectedOrders）
     */
    @Override
    public MatchingResult submitOrderV2(Order order) {
        String symbolCode = order.getSymbol().code();
        OrderBook orderBook = book(symbolCode);

        // 本次撮合產生的成交事件
        final List<TradeExecuted> trades = new ArrayList<>();

        // 受影響訂單集合：
        // - 使用 LinkedHashSet 保持插入順序
        // - 同時避免同一張單被重複加入
        final LinkedHashSet<Order> affectedOrders = new LinkedHashSet<>();
        affectedOrders.add(order); // 新單一定受影響

        synchronized (orderBook) {
            // 記錄本輪最後成交價，供 MARKET 殘量轉 LIMIT 掛簿時使用
            BigDecimal lastExecPrice = null;

            // TODO: 依 symbol 的 priceScale / qtyScale 對價格與數量做量化
            // TODO: 加入自成交防護（Self Match Prevention）
            // TODO: 支援 IOC / FOK / POST_ONLY / REDUCE_ONLY

            if (order.getSide() == OrderSide.BUY) {
                // BUY：買單吃賣簿
                while (orderBook.peekBestAsk() != null
                        && order.getQty().signum() > 0
                        && order.getPrice() != null
                        && order.getPrice().compareTo(orderBook.peekBestAsk().getPrice()) >= 0) {

                    Order bestAsk = orderBook.pollBestAsk();
                    affectedOrders.add(bestAsk);

                    // 本次實際成交量 = taker 剩餘量 與 maker 剩餘量 取最小值
                    BigDecimal execQty = order.getQty().min(bestAsk.getQty());

                    // BUY 吃 SELL 時，成交價取 maker（賣簿）價格
                    BigDecimal execPx = bestAsk.getPrice();

                    Instant now = Instant.now();

                    // 對 taker（BUY 方）而言，成交數量為正
                    trades.add(new TradeExecuted(
                            order.getUid(),
                            order.getSymbol(),
                            execQty,
                            execPx,
                            0L,
                            now
                    ));

                    // 對 maker（SELL 方）而言，成交數量為負
                    trades.add(new TradeExecuted(
                            bestAsk.getUid(),
                            bestAsk.getSymbol(),
                            execQty.negate(),
                            execPx,
                            0L,
                            now
                    ));

                    lastExecPrice = execPx;

                    // 更新雙方訂單狀態與成交資訊
                    order.fill(execQty, execPx);
                    bestAsk.fill(execQty, execPx);

                    // 若對手單尚未完全成交，放回簿中
                    if (bestAsk.getQty().signum() > 0) {
                        orderBook.add(bestAsk);
                    }
                }

                // BUY 單有殘量時的處理
                if (order.getQty().signum() > 0) {
                    if (order.getType() == OrderType.MARKET) {
                        // MARKET 殘量 → 轉 LIMIT 掛簿（簡化版 MTL）
                        BigDecimal postPrice = choosePostPriceForBuy(orderBook, order, lastExecPrice);
                        order.setType(OrderType.LIMIT);
                        order.setPrice(postPrice);
                        orderBook.add(order);

                        // TODO: 通知上層重新評估保證金與費率凍結
                    } else {
                        // LIMIT 吃不完 → 原樣掛簿
                        orderBook.add(order);

                        // TODO: 通知上層凍結剩餘委託對應 IM / fee
                    }
                }

            } else {
                // SELL：賣單吃買簿
                while (orderBook.peekBestBid() != null
                        && order.getQty().signum() > 0
                        && order.getPrice() != null
                        && order.getPrice().compareTo(orderBook.peekBestBid().getPrice()) <= 0) {

                    Order bestBid = orderBook.pollBestBid();
                    affectedOrders.add(bestBid);

                    // 本次實際成交量 = taker 剩餘量 與 maker 剩餘量 取最小值
                    BigDecimal execQty = order.getQty().min(bestBid.getQty());

                    // SELL 吃 BUY 時，成交價取 maker（買簿）價格
                    BigDecimal execPx = bestBid.getPrice();

                    Instant now = Instant.now();

                    // 對 taker（SELL 方）而言，成交數量為負
                    trades.add(new TradeExecuted(
                            order.getUid(),
                            order.getSymbol(),
                            execQty.negate(),
                            execPx,
                            0L,
                            now
                    ));

                    // 對 maker（BUY 方）而言，成交數量為正
                    trades.add(new TradeExecuted(
                            bestBid.getUid(),
                            bestBid.getSymbol(),
                            execQty,
                            execPx,
                            0L,
                            now
                    ));

                    lastExecPrice = execPx;

                    // 更新雙方訂單狀態與成交資訊
                    order.fill(execQty, execPx);
                    bestBid.fill(execQty, execPx);

                    // 若對手單尚未完全成交，放回簿中
                    if (bestBid.getQty().signum() > 0) {
                        orderBook.add(bestBid);
                    }
                }

                // SELL 單有殘量時的處理
                if (order.getQty().signum() > 0) {
                    if (order.getType() == OrderType.MARKET) {
                        // MARKET 殘量 → 轉 LIMIT 掛簿（簡化版 MTL）
                        BigDecimal postPrice = choosePostPriceForSell(orderBook, order, lastExecPrice);
                        order.setType(OrderType.LIMIT);
                        order.setPrice(postPrice);
                        orderBook.add(order);

                        // TODO: 通知上層重新評估保證金與費率凍結
                    } else {
                        // LIMIT 吃不完 → 原樣掛簿
                        orderBook.add(order);

                        // TODO: 通知上層凍結剩餘委託對應 IM / fee
                    }
                }
            }
        }

        return MatchingResult.builder()
                .trades(trades)
                .affectedOrders(new ArrayList<>(affectedOrders))
                .build();
    }

    // -------------------------------------------------
    // 其他行為：撤單 / 查詢
    // -------------------------------------------------

    /**
     * 取消訂單
     * -------------------------------------------------
     * 功能：
     * - 從指定交易對的訂單簿中移除該訂單
     *
     * 注意：
     * - 此方法僅從 order book 移除訂單，不直接改 Order 狀態
     * - 狀態更新（例如設成 CANCELED）應交由上層 OrderService 處理
     *
     * @param order 欲取消的訂單
     * @return true 表示成功從訂單簿移除；false 表示找不到或未成功移除
     */
    @Override
    public boolean cancelOrder(Order order) {
        OrderBook orderBook = books.get(order.getSymbol().code());
        if (orderBook == null) return false;

        synchronized (orderBook) {
            // TODO: 取消成功後通知上層釋放剩餘凍結（IM + 未使用費率預留）
            return orderBook.cancel(order);
        }
    }

    /**
     * 取得訂單簿快照
     *
     * @param symbolCode 交易對代碼
     * @param depth      返回檔位深度
     * @return 指定交易對的訂單簿快照
     */
    @Override
    public OrderBookSnapshot snapshot(String symbolCode, int depth) {
        OrderBook orderBook = books.get(symbolCode);
        if (orderBook == null) {
            return new OrderBookSnapshot(List.of(), List.of());
        }

        synchronized (orderBook) {
            return orderBook.snapshot(Math.max(1, depth));
        }
    }

    /**
     * 取得最優買賣價（Top of Book）
     *
     * @param symbolCode 交易對代碼
     * @return 最優買價 / 最優賣價；若此簿不存在或無掛單，回傳 Optional.empty()
     */
    @Override
    public Optional<TopOfBook> top(String symbolCode) {
        OrderBook orderBook = books.get(symbolCode);
        if (orderBook == null) return Optional.empty();

        synchronized (orderBook) {
            BigDecimal bestBid = (orderBook.peekBestBid() != null)
                    ? orderBook.peekBestBid().getPrice()
                    : null;

            BigDecimal bestAsk = (orderBook.peekBestAsk() != null)
                    ? orderBook.peekBestAsk().getPrice()
                    : null;

            if (bestBid == null && bestAsk == null) {
                return Optional.empty();
            }

            return Optional.of(
                    TopOfBook.builder()
                            .bestBid(bestBid)
                            .bestAsk(bestAsk)
                            .build()
            );
        }
    }

    // -------------------------------------------------
    // MARKET 殘量轉 LIMIT 掛簿（MTL）掛價策略
    // -------------------------------------------------

    /**
     * BUY 單在 MARKET 殘量情況下的掛簿價格選擇策略
     * -------------------------------------------------
     * 優先順序：
     * 1) 本輪最後成交價
     * 2) 當前最佳買價 bestBid
     * 3) 當前最佳賣價 bestAsk
     * 4) 保留原始價格（通常是市價模擬時的極端價格）
     *
     * @param orderBook      該交易對的訂單簿
     * @param order          新單
     * @param lastExecPrice  本輪最後成交價
     * @return 應掛簿的 LIMIT 價格
     */
    private BigDecimal choosePostPriceForBuy(OrderBook orderBook, Order order, BigDecimal lastExecPrice) {
        if (lastExecPrice != null) return lastExecPrice;

        Order bestBid = orderBook.peekBestBid();
        if (bestBid != null) return bestBid.getPrice();

        Order bestAsk = orderBook.peekBestAsk();
        if (bestAsk != null) return bestAsk.getPrice();

        return order.getPrice();
    }

    /**
     * SELL 單在 MARKET 殘量情況下的掛簿價格選擇策略
     * -------------------------------------------------
     * 優先順序：
     * 1) 本輪最後成交價
     * 2) 當前最佳賣價 bestAsk
     * 3) 當前最佳買價 bestBid
     * 4) 保留原始價格
     *
     * @param orderBook      該交易對的訂單簿
     * @param order          新單
     * @param lastExecPrice  本輪最後成交價
     * @return 應掛簿的 LIMIT 價格
     */
    private BigDecimal choosePostPriceForSell(OrderBook orderBook, Order order, BigDecimal lastExecPrice) {
        if (lastExecPrice != null) return lastExecPrice;

        Order bestAsk = orderBook.peekBestAsk();
        if (bestAsk != null) return bestAsk.getPrice();

        Order bestBid = orderBook.peekBestBid();
        if (bestBid != null) return bestBid.getPrice();

        return order.getPrice();
    }
}