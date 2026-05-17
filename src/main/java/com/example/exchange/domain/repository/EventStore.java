/*
 * 檔案用途：Repository 介面，定義領域層需要的資料存取契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.event.TradeExecuted;

import java.util.List;

/**
 * 事件儲存（Event Store）的抽象
 *
 * - 用於保存可回放的交易事件（例如成交）
 * - 實作可用 Kafka（壓實/compacted topic）、或資料庫（append-only）
 * - 需提供單調遞增的 seq（去重/回放定位）
 */
public interface EventStore {

    /**
     * 追加事件並回傳事件序號 seq
     * - 實作方需保證 seq 單調遞增（全域或按 uid/shard）
     */
    long append(TradeExecuted event);

    /**
     * 查詢某使用者的最後事件序號（恢復時決定從哪個 seq 之後開始 replay）
     */
    long lastSeq(long uid);

    /**
     * 查詢指定 uid 在 afterSeq 之後的事件，用於 snapshot replay。
     */
    default List<TradeExecuted> fetchAfter(long uid, long afterSeq, int limit) {
        return List.of();
    }
}
