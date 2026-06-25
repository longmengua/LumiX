/*
 * 檔案用途：領域 DTO，承載 matching event log 的單筆可 replay / audit 事件。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.event.TradeExecuted;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


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
@Data
@Builder
@Jacksonized
public class MatchingEventLogEntry {

    private final String symbolCode;

    private final long offset;

    private final long commandOffset;

    private final TradeExecuted trade;

    private final String ownerId;

    private final long ownerEpoch;

    private final Instant createdAt;
    public MatchingEventLogEntry(String symbolCode, long offset, long commandOffset, TradeExecuted trade, String ownerId, long ownerEpoch, Instant createdAt) {
        this.symbolCode = symbolCode;
        this.offset = offset;
        this.commandOffset = commandOffset;
        this.trade = trade;
        this.ownerId = ownerId;
        this.ownerEpoch = ownerEpoch;
        this.createdAt = createdAt;
    }

    public String symbolCode() {
        return symbolCode;
    }

    public long offset() {
        return offset;
    }

    public long commandOffset() {
        return commandOffset;
    }

    public TradeExecuted trade() {
        return trade;
    }

    public String ownerId() {
        return ownerId;
    }

    public long ownerEpoch() {
        return ownerEpoch;
    }

    public Instant createdAt() {
        return createdAt;
    }
}