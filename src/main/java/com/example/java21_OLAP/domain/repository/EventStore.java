package com.example.java21_OLAP.domain.repository;

import com.example.java21_OLAP.domain.event.TradeExecuted;

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
}
