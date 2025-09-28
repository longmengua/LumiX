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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory 撮合引擎（多交易對）
 *
 * 功能重點：
 * 1) 先以「價格撮合」處理傳入訂單（taker）
 * 2) 若為 MARKET 且未完全成交 → 轉為 LIMIT 掛簿（MTL）
 *    - 掛簿價優先取「最後成交價」，否則取該側合理的 Top-of-Book 價位
 *
 * 相容性：
 * - submitOrderV2(Order) 回傳 MatchingResult（含 affectedOrders）
 * - submitOrder(Order) 仍保留，委派至 V2 結果的 trades
 */
@Component
public class InMemoryMatchingEngine implements MatchingEngine {

    /** key = symbolCode（例：BTCUSDT） */
    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();

    private OrderBook book(String symbolCode) {
        return books.computeIfAbsent(symbolCode, k -> new OrderBook());
    }

    // ------------------------- 相容舊版 API -------------------------

    @Override
    public List<TradeExecuted> submitOrder(Order order) {
        // 舊版僅需成交事件；新版則同時帶 affectedOrders
        return submitOrderV2(order).getTrades();
    }

    // ------------------------- 新版 API -------------------------

    @Override
    public MatchingResult submitOrderV2(Order order) {
        String sym = order.getSymbol().code();
        OrderBook ob = book(sym);

        final List<TradeExecuted> trades = new ArrayList<>();
        final LinkedHashSet<Order> affectedOrders = new LinkedHashSet<>(); // 去重、保序
        affectedOrders.add(order); // 新單一定受影響（狀態/剩餘量可能變動）

        synchronized (ob) {
            BigDecimal lastExecPrice = null; // 記錄本輪最後成交價（供 MTL 掛價）

            // TODO: 價格/數量量化：先按 symbol priceScale/qtyScale 做 round/scale
            // TODO: 自成交防護（SMP）：若對手簿最佳單同 uid → 依策略取消/跳過/改價

            if (order.getSide() == OrderSide.BUY) {
                // BUY：買單 vs 賣簿撮合（買價 >= 最佳賣價）
                while (ob.peekBestAsk() != null
                        && order.getQty().signum() > 0
                        && order.getPrice() != null
                        && order.getPrice().compareTo(ob.peekBestAsk().getPrice()) >= 0) {

                    Order bestAsk = ob.pollBestAsk();
                    affectedOrders.add(bestAsk); // 對手單必定受影響

                    BigDecimal execQty = order.getQty().min(bestAsk.getQty());
                    BigDecimal execPx  = bestAsk.getPrice();

                    // TODO: maker/taker 角色標記（供費率）；此處新單多為 taker，簿中單為 maker
                    // TODO: TradeExecuted 可擴充 role/fee 欄位（或交由上層計算）

                    trades.add(new TradeExecuted(
                            order.getUid(), order.getSymbol(), execQty, execPx, 0L, Instant.now()));
                    trades.add(new TradeExecuted(
                            bestAsk.getUid(), bestAsk.getSymbol(), execQty.negate(), execPx, 0L, Instant.now()));

                    lastExecPrice = execPx;

                    order.fill(execQty);
                    bestAsk.fill(execQty);

                    if (bestAsk.getQty().signum() > 0) {
                        ob.add(bestAsk);            // 部分成交 → 放回簿中
                        // affectedOrders 已加過 bestAsk；此處不重複加
                    }
                }

                // 殘量處理（BUY）
                if (order.getQty().signum() > 0) {
                    if (order.getType() == OrderType.MARKET) {
                        // ✅ MTL：市價殘量 → 轉 LIMIT 掛簿
                        BigDecimal postPx = choosePostPriceForBuy(ob, order, lastExecPrice);
                        order.setType(OrderType.LIMIT);
                        order.setPrice(postPx);
                        ob.add(order);
                        // TODO: 通知上層依新掛簿價重估 IM & 手續費凍結
                    } else {
                        ob.add(order); // LIMIT 吃不完 → 續掛
                        // TODO: 通知上層凍結 IM（若先前只凍結 taker 上限需調整）
                    }
                }

            } else {
                // SELL：賣單 vs 買簿撮合（賣價 <= 最佳買價）
                while (ob.peekBestBid() != null
                        && order.getQty().signum() > 0
                        && order.getPrice() != null
                        && order.getPrice().compareTo(ob.peekBestBid().getPrice()) <= 0) {

                    Order bestBid = ob.pollBestBid();
                    affectedOrders.add(bestBid); // 對手單受影響

                    BigDecimal execQty = order.getQty().min(bestBid.getQty());
                    BigDecimal execPx  = bestBid.getPrice();

                    // TODO: maker/taker 角色標記（供費率）
                    trades.add(new TradeExecuted(
                            order.getUid(), order.getSymbol(), execQty.negate(), execPx, 0L, Instant.now()));
                    trades.add(new TradeExecuted(
                            bestBid.getUid(), bestBid.getSymbol(), execQty, execPx, 0L, Instant.now()));

                    lastExecPrice = execPx;

                    order.fill(execQty);
                    bestBid.fill(execQty);

                    if (bestBid.getQty().signum() > 0) {
                        ob.add(bestBid);          // 部分成交 → 放回簿中
                    }
                }

                // 殘量處理（SELL）
                if (order.getQty().signum() > 0) {
                    if (order.getType() == OrderType.MARKET) {
                        BigDecimal postPx = choosePostPriceForSell(ob, order, lastExecPrice);
                        order.setType(OrderType.LIMIT);
                        order.setPrice(postPx);
                        ob.add(order);
                        // TODO: 通知上層依新掛簿價重估 IM & 手續費凍結
                    } else {
                        ob.add(order);
                        // TODO: 通知上層凍結 IM
                    }
                }
            }
        }

        return MatchingResult.builder().trades(trades).affectedOrders(new ArrayList<Order>(affectedOrders)).build();
    }

    // ------------------------- 其他行為：取消/查詢 -------------------------

    @Override
    public boolean cancelOrder(Order order) {
        OrderBook ob = books.get(order.getSymbol().code());
        if (ob == null) return false;
        synchronized (ob) {
            // TODO: 取消成功後通知上層釋放剩餘凍結（IM + 未用手續費）
            return ob.cancel(order);
        }
    }

    @Override
    public OrderBookSnapshot snapshot(String symbolCode, int depth) {
        OrderBook ob = books.get(symbolCode);
        if (ob == null) {
            return new OrderBookSnapshot(Collections.emptyList(), Collections.emptyList());
        }
        synchronized (ob) {
            return ob.snapshot(Math.max(1, depth));
        }
    }

    @Override
    public Optional<TopOfBook> top(String symbolCode) {
        OrderBook ob = books.get(symbolCode);
        if (ob == null) return Optional.empty();
        synchronized (ob) {
            BigDecimal bid = (ob.peekBestBid() != null) ? ob.peekBestBid().getPrice() : null;
            BigDecimal ask = (ob.peekBestAsk() != null) ? ob.peekBestAsk().getPrice() : null;
            if (bid == null && ask == null) return Optional.empty();
            return Optional.of(TopOfBook.builder().bestAsk(ask).bestBid(bid).build());
        }
    }

    // ------------------------- 掛價策略（MTL） -------------------------

    /**
     * 買單 MTL 的掛簿價格選擇邏輯
     * 優先序：1) lastExecPrice 2) bestBid 3) bestAsk 4) retain original (市價單應已設極端價)
     */
    private BigDecimal choosePostPriceForBuy(OrderBook ob, Order order, BigDecimal lastExecPrice) {
        if (lastExecPrice != null) return lastExecPrice;
        Order bestBid = ob.peekBestBid();
        if (bestBid != null) return bestBid.getPrice();
        Order bestAsk = ob.peekBestAsk();
        if (bestAsk != null) return bestAsk.getPrice();
        return order.getPrice();
    }

    /**
     * 賣單 MTL 的掛簿價格選擇邏輯
     * 優先序：1) lastExecPrice 2) bestAsk 3) bestBid 4) retain original
     */
    private BigDecimal choosePostPriceForSell(OrderBook ob, Order order, BigDecimal lastExecPrice) {
        if (lastExecPrice != null) return lastExecPrice;
        Order bestAsk = ob.peekBestAsk();
        if (bestAsk != null) return bestAsk.getPrice();
        Order bestBid = ob.peekBestBid();
        if (bestBid != null) return bestBid.getPrice();
        return order.getPrice();
    }
}
