package com.example.exchange.domain.service;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.MatchingResult;
import com.example.exchange.domain.model.Order;
import com.example.exchange.domain.model.TopOfBook;

import java.util.List;
import java.util.Optional;

/**
 * 撮合引擎抽象（Domain Service 介面）
 *
 * 定位：
 * - 僅定義撮合行為與查詢視圖的「業務契約」，不關心底層資料結構與併發細節（由 infra 層提供）。
 * - 不處理資金檢查、保證金、手續費與資金費結算（交由 Application 層或 Account/Fee/Funding/Risk 服務）。
 *
 * 功能：
 * - submitOrder：提交新訂單，回傳成交事件列表（相容舊版）
 * - submitOrderV2：提交新訂單，回傳更完整的撮合結果（trades + affectedOrders）
 * - cancelOrder：取消訂單
 * - snapshot：取得訂單簿快照
 * - top：取得最優買/賣價
 */
public interface MatchingEngine {

    /**
     * 提交新訂單並嘗試撮合（舊版，相容用）。
     *
     * @param order 新訂單（LIMIT 或 MARKET）
     * @return 撮合產生的成交事件（可能為空）
     */
    List<TradeExecuted> submitOrder(Order order);

    /**
     * 提交新訂單並嘗試撮合（新版）。
     *
     * @param order 新訂單
     * @return 撮合結果（含成交事件與受影響的訂單集合）
     */
    MatchingResult submitOrderV2(Order order);

    /**
     * 取消訂單。
     * @param order 欲取消的訂單
     * @return 是否成功取消
     */
    boolean cancelOrder(Order order);

    /**
     * 取得某 symbol 的訂單簿快照（聚合到價位）。
     *
     * @param symbolCode 交易對代號（例：BTCUSDT）
     * @param depth 最多返回幾檔
     * @return 聚合快照
     */
    OrderBookSnapshot snapshot(String symbolCode, int depth);

    /**
     * 取得該 symbol 的最優買/賣價（Top-of-Book）。
     *
     * @param symbolCode 交易對代號
     * @return 最優買/賣價（簿為空則 empty）
     */
    Optional<TopOfBook> top(String symbolCode);
}
