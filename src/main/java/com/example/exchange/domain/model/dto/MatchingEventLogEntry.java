/*
 * 檔案用途：領域 DTO，承載 matching event log 的單筆可 replay / audit 事件。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.event.TradeExecuted;

import java.time.Instant;

/**
 * 單一 symbol 的 matching event log entry。
 *
 * @param symbolCode    normalized symbol code
 * @param offset        per-symbol event offset
 * @param commandOffset 產生此 event 時對應的 command offset
 * @param trade         撮合產生的成交事件
 * @param ownerId       寫入 event 時的 sequencer owner；in-memory baseline 可為 null
 * @param ownerEpoch    寫入 event 時的 sequencer epoch；未接 fencing 時為 0
 * @param createdAt     event log 建立時間
 */
public record MatchingEventLogEntry(
        String symbolCode,
        long offset,
        long commandOffset,
        TradeExecuted trade,
        String ownerId,
        long ownerEpoch,
        Instant createdAt
) {
}
