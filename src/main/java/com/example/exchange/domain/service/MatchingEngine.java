/*
 * 檔案用途：領域服務，封裝撮合、風控、Polymarket 同步與交易規則。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.MatchingResult;
import com.example.exchange.domain.model.dto.MatchingEngineSnapshot;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.dto.TopOfBook;

import java.math.BigDecimal;
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
     * 修改仍在簿內的掛單價格或剩餘數量。
     *
     * @param order    欲修改的訂單
     * @param newPrice 新價格
     * @param newQty   新剩餘數量
     * @return 是否成功修改
     */
    boolean amendOrder(Order order, BigDecimal newPrice, BigDecimal newQty);

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

    /**
     * 匯出單一交易對的撮合狀態快照。
     *
     * @param symbolCode 交易對代碼
     * @return 可用於 restore 的 in-memory snapshot
     */
    MatchingEngineSnapshot exportSnapshot(String symbolCode);

    /**
     * 還原單一交易對的撮合狀態快照。
     *
     * @param snapshot 先前匯出的撮合快照
     */
    void restoreSnapshot(MatchingEngineSnapshot snapshot);
}
