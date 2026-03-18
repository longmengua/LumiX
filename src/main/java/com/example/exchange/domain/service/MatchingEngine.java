package com.example.exchange.domain.service;

import com.example.exchange.domain.model.MatchingResult;
import com.example.exchange.domain.model.Order;
import com.example.exchange.domain.model.TopOfBook;

import java.util.Optional;

/**
 * 撮合引擎抽象（Domain Service）
 * -------------------------------------------------
 *
 * 定位：
 * - 定義撮合行為與查詢視圖的業務契約
 * - 不關心底層資料結構、執行緒模型與儲存方式
 *
 * 目前能力：
 * - submit：提交新訂單並撮合
 * - cancelOrder：取消訂單
 * - snapshot：取得訂單簿快照
 * - top：取得最優買賣價
 */
public interface MatchingEngine {

    /**
     * 提交新訂單並執行撮合
     *
     * @param order 新訂單
     * @return 撮合結果（成交事件 + 受影響訂單）
     */
    MatchingResult submit(Order order);

    /**
     * 取消訂單
     *
     * @param order 欲取消的訂單
     * @return 是否成功取消
     */
    boolean cancelOrder(Order order);

    /**
     * 取得某交易對的訂單簿快照
     *
     * @param symbolCode 交易對代碼
     * @param depth      深度
     * @return 訂單簿快照
     */
    OrderBookSnapshot snapshot(String symbolCode, int depth);

    /**
     * 取得最優買賣價
     *
     * @param symbolCode 交易對代碼
     * @return Top of Book；若無資料則 empty
     */
    Optional<TopOfBook> top(String symbolCode);
}