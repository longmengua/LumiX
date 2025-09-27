package com.example.exchange.domain.service;

import com.example.exchange.domain.model.Order;
import com.example.exchange.domain.event.TradeExecuted;

import java.util.List;
import java.util.Optional;

/**
 * 撮合引擎抽象（Domain Service 介面）
 *
 * - submitOrder：提交新訂單，嘗試撮合並回傳「成交事件列表」
 * - cancelOrder：取消訂單
 *
 * 注意：這裡只定義行為，不碰技術細節（資料結構、併發控制在 infra 實作）。
 */
public interface MatchingEngine {

    /**
     * 提交新訂單並嘗試撮合。
     * @param order  新訂單（通常為 LIMIT；若是 MARKET 可用極端價格模擬吃單）
     * @return       撮合產生的成交事件（可能為空）
     */
    List<TradeExecuted> submitOrder(Order order);

    /**
     * 取消訂單。
     * @param order 欲取消的訂單
     * @return 成功/失敗
     */
    boolean cancelOrder(Order order);

    /**
     * 取得某 symbol 的訂單簿快照（聚合到價位），限制深度 depth。
     */
    OrderBookSnapshot snapshot(String symbolCode, int depth);

    /**
     * 取得該 symbol 的最優買/賣價（Top-of-Book）。
     */
    Optional<TopOfBook> top(String symbolCode);

    /** Top-of-Book 結構 */
    record TopOfBook(java.math.BigDecimal bestBid, java.math.BigDecimal bestAsk) {}
}
