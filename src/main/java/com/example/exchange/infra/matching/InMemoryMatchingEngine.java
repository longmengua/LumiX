package com.example.exchange.infra.matching;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.MatchingResult;
import com.example.exchange.domain.model.Order;
import com.example.exchange.domain.model.OrderSide;
import com.example.exchange.domain.model.OrderType;
import com.example.exchange.domain.model.TimeInForce;
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
 * - 此類別是 MatchingEngine 的一個簡化版實作，負責在記憶體中維護多個交易對的訂單簿，
 *   並對新訂單執行撮合。
 * - 每個 symbol（例如 BTCUSDT）對應一個獨立的 OrderBook。
 *
 * 目前支援：
 * 1) LIMIT / MARKET 訂單
 * 2) GTC / IOC / FOK
 * 3) 取消訂單
 * 4) 訂單簿快照與 Top-of-Book 查詢
 *
 * 設計原則（本版）：
 * - LIMIT 單：
 *   - 依價格條件撮合
 *   - 在 GTC 下，若有剩餘量，可繼續掛簿
 *
 * - MARKET 單：
 *   - 不帶價格限制
 *   - 只吃現有對手盤
 *   - 吃不完的剩餘量直接失效
 *   - 不應進入 order book
 *
 * TimeInForce 規則（本版）：
 * - LIMIT + GTC：剩餘量可掛簿
 * - LIMIT + IOC：立即成交可成交部分，剩餘量失效
 * - LIMIT + FOK：若不能一次全部成交，整張直接失效，不產生成交
 * - MARKET：只吃現有對手盤，不掛簿；若剩餘量未成交，直接失效
 *
 * 目前限制：
 * - 僅為 Demo 級撮合，不是生產級撮合核心
 * - 尚未完整支援：
 *   - 價格/數量量化
 *   - 價格優先 + 時間優先（目前只有價格優先）
 *   - Self Match Prevention
 *   - POST_ONLY / REDUCE_ONLY
 *   - maker/taker fee
 *   - 更高效的單執行緒序列器模型
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
     * 取得指定交易對的訂單簿
     * - 若不存在則自動建立
     *
     * @param symbolCode 交易對代碼
     * @return 對應的 OrderBook
     */
    private OrderBook book(String symbolCode) {
        return books.computeIfAbsent(symbolCode, k -> new OrderBook());
    }

    /**
     * 提交新訂單並執行撮合
     * -------------------------------------------------
     *
     * 核心流程：
     * 1) 依 symbol 取得訂單簿
     * 2) 若為 FOK，先預估是否可完全成交
     * 3) 依 BUY / SELL 分別對對手簿撮合
     * 4) 根據 TimeInForce 與訂單型別處理剩餘量
     * 5) 回傳 trades + affectedOrders
     *
     * @param order 新訂單
     * @return 撮合結果
     */
    @Override
    public MatchingResult submit(Order order) {
        String symbolCode = order.getSymbol().code();
        OrderBook orderBook = book(symbolCode);

        final List<TradeExecuted> trades = new ArrayList<>();
        final LinkedHashSet<Order> affectedOrders = new LinkedHashSet<>();
        affectedOrders.add(order);

        synchronized (orderBook) {
            // TODO: 依 symbol 的 priceScale / qtyScale 對價格與數量做量化
            // TODO: 自成交防護（Self Match Prevention）
            // TODO: 支援 POST_ONLY / REDUCE_ONLY

            TimeInForce tif = (order.getTimeInForce() == null)
                    ? TimeInForce.GTC
                    : order.getTimeInForce();

            /**
             * FOK（Fill Or Kill）
             * -------------------------------------------------
             * 若不能一次全部成交，則整張失效，不成交、不掛簿。
             *
             * 注意：
             * - 這裡對 LIMIT / MARKET 都適用
             * - MARKET FOK 的意思是：必須在當前簿況下一次吃滿，否則整張失效
             */
            if (tif == TimeInForce.FOK) {
                BigDecimal matchable = orderBook.matchableQty(order);
                if (matchable.compareTo(order.getQty()) < 0) {
                    order.expire();
                    return MatchingResult.builder()
                            .trades(List.of())
                            .affectedOrders(new ArrayList<>(affectedOrders))
                            .build();
                }
            }

            if (order.getSide() == OrderSide.BUY) {
                matchBuy(orderBook, order, trades, affectedOrders);
            } else {
                matchSell(orderBook, order, trades, affectedOrders);
            }

            /**
             * 剩餘量處理
             * -------------------------------------------------
             * - 若訂單已全成，無需處理
             * - 若仍有剩餘量：
             *   - IOC 直接失效
             *   - FOK 理論上不應走到這裡（前面已預檢），此處保留防守性處理
             *   - GTC：
             *       * LIMIT 可掛簿
             *       * MARKET 不掛簿，直接失效
             */
            if (order.getQty().signum() > 0) {
                switch (tif) {
                    case IOC -> order.expire();
                    case FOK -> order.expire(); // 防守性處理
                    case GTC -> handleRemainderForGtc(orderBook, order);
                }
            }
        }

        return MatchingResult.builder()
                .trades(trades)
                .affectedOrders(new ArrayList<>(affectedOrders))
                .build();
    }

    /**
     * BUY 單撮合邏輯
     * -------------------------------------------------
     * - BUY 單會去吃 asks（賣簿）
     * - LIMIT BUY：必須滿足 buyPrice >= bestAsk.price
     * - MARKET BUY：只要有 asks 就持續吃
     *
     * @param orderBook      訂單簿
     * @param order          新買單
     * @param trades         成交事件輸出列表
     * @param affectedOrders 受影響訂單集合
     */
    private void matchBuy(OrderBook orderBook,
                          Order order,
                          List<TradeExecuted> trades,
                          LinkedHashSet<Order> affectedOrders) {

        while (canMatchBuy(order, orderBook)) {
            Order bestAsk = orderBook.pollBestAsk();
            affectedOrders.add(bestAsk);

            BigDecimal execQty = order.getQty().min(bestAsk.getQty());
            BigDecimal execPx = bestAsk.getPrice();
            Instant now = Instant.now();

            // taker（BUY 方）成交量為正
            trades.add(new TradeExecuted(
                    order.getUid(),
                    order.getSymbol(),
                    execQty,
                    execPx,
                    0L,
                    now
            ));

            // maker（SELL 方）成交量為負
            trades.add(new TradeExecuted(
                    bestAsk.getUid(),
                    bestAsk.getSymbol(),
                    execQty.negate(),
                    execPx,
                    0L,
                    now
            ));

            order.fill(execQty, execPx);
            bestAsk.fill(execQty, execPx);

            // 對手單若尚未完全成交，放回簿中
            if (bestAsk.getQty().signum() > 0) {
                orderBook.add(bestAsk);
            }
        }
    }

    /**
     * SELL 單撮合邏輯
     * -------------------------------------------------
     * - SELL 單會去吃 bids（買簿）
     * - LIMIT SELL：必須滿足 sellPrice <= bestBid.price
     * - MARKET SELL：只要有 bids 就持續吃
     *
     * @param orderBook      訂單簿
     * @param order          新賣單
     * @param trades         成交事件輸出列表
     * @param affectedOrders 受影響訂單集合
     */
    private void matchSell(OrderBook orderBook,
                           Order order,
                           List<TradeExecuted> trades,
                           LinkedHashSet<Order> affectedOrders) {

        while (canMatchSell(order, orderBook)) {
            Order bestBid = orderBook.pollBestBid();
            affectedOrders.add(bestBid);

            BigDecimal execQty = order.getQty().min(bestBid.getQty());
            BigDecimal execPx = bestBid.getPrice();
            Instant now = Instant.now();

            // taker（SELL 方）成交量為負
            trades.add(new TradeExecuted(
                    order.getUid(),
                    order.getSymbol(),
                    execQty.negate(),
                    execPx,
                    0L,
                    now
            ));

            // maker（BUY 方）成交量為正
            trades.add(new TradeExecuted(
                    bestBid.getUid(),
                    bestBid.getSymbol(),
                    execQty,
                    execPx,
                    0L,
                    now
            ));

            order.fill(execQty, execPx);
            bestBid.fill(execQty, execPx);

            // 對手單若尚未完全成交，放回簿中
            if (bestBid.getQty().signum() > 0) {
                orderBook.add(bestBid);
            }
        }
    }

    /**
     * 判斷 BUY 單是否可與當前最佳賣單成交
     * -------------------------------------------------
     * 規則：
     * - 若賣簿為空，不能成交
     * - 若剩餘量 <= 0，不能成交
     * - MARKET BUY：只要有賣簿就能吃
     * - LIMIT BUY：需滿足 buyPrice >= bestAsk.price
     *
     * @param order     傳入訂單
     * @param orderBook 訂單簿
     * @return 是否可成交
     */
    private boolean canMatchBuy(Order order, OrderBook orderBook) {
        if (orderBook.peekBestAsk() == null || order.getQty().signum() <= 0) {
            return false;
        }

        if (order.getType() == OrderType.MARKET) {
            return true;
        }

        return order.getPrice() != null
                && order.getPrice().compareTo(orderBook.peekBestAsk().getPrice()) >= 0;
    }

    /**
     * 判斷 SELL 單是否可與當前最佳買單成交
     * -------------------------------------------------
     * 規則：
     * - 若買簿為空，不能成交
     * - 若剩餘量 <= 0，不能成交
     * - MARKET SELL：只要有買簿就能吃
     * - LIMIT SELL：需滿足 sellPrice <= bestBid.price
     *
     * @param order     傳入訂單
     * @param orderBook 訂單簿
     * @return 是否可成交
     */
    private boolean canMatchSell(Order order, OrderBook orderBook) {
        if (orderBook.peekBestBid() == null || order.getQty().signum() <= 0) {
            return false;
        }

        if (order.getType() == OrderType.MARKET) {
            return true;
        }

        return order.getPrice() != null
                && order.getPrice().compareTo(orderBook.peekBestBid().getPrice()) <= 0;
    }

    /**
     * GTC 訂單剩餘量處理
     * -------------------------------------------------
     * 規則：
     * - LIMIT：剩餘量直接掛簿
     * - MARKET：不應掛簿，剩餘量直接失效
     *
     * @param orderBook 訂單簿
     * @param order     尚有剩餘量的訂單
     */
    private void handleRemainderForGtc(OrderBook orderBook, Order order) {
        if (order.getType() == OrderType.MARKET) {
            // 正統市場單不應掛簿；若流動性不足，剩餘量直接失效
            order.expire();

            // TODO: 通知上層釋放未使用的凍結資金
            return;
        }

        // LIMIT 剩餘量直接掛簿
        orderBook.add(order);

        // TODO: 通知上層凍結剩餘委託對應 IM / fee
    }

    // -------------------------------------------------
    // 其他行為：撤單 / 查詢
    // -------------------------------------------------

    /**
     * 取消訂單
     * -------------------------------------------------
     * 功能：
     * - 從對應交易對的 order book 中移除該訂單
     *
     * 注意：
     * - 此方法僅處理「從簿中移除」
     * - Order 狀態更新（例如設為 CANCELED）應由上層服務處理
     *
     * @param order 欲取消的訂單
     * @return true 表示成功移除；false 表示未找到或簿不存在
     */
    @Override
    public boolean cancelOrder(Order order) {
        OrderBook orderBook = books.get(order.getSymbol().code());
        if (orderBook == null) return false;

        synchronized (orderBook) {
            // TODO: 取消成功後通知上層釋放剩餘凍結（IM + 未用手續費）
            return orderBook.cancel(order);
        }
    }

    /**
     * 取得訂單簿快照
     *
     * @param symbolCode 交易對代碼
     * @param depth      返回深度
     * @return 該交易對的訂單簿快照；若不存在則回傳空快照
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
     * @return 最優買價 / 最優賣價；若無資料則回傳 empty
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
}