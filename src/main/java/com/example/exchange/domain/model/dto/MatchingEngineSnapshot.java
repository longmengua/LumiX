/*
 * 檔案用途：領域 DTO，承載 matching engine 可恢復狀態快照。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.dto.Order;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * 單一 symbol 的撮合狀態快照。
 *
 * @param symbolCode    normalized symbol code
 * @param matchSequence 已分配的最後 match sequence
 * @param commandOffset snapshot 對應的最後 command offset
 * @param eventOffset   snapshot 對應的最後 event offset
 * @param bids          bid book resting orders，依撮合優先序排列
 * @param asks          ask book resting orders，依撮合優先序排列
 * @param createdAt     snapshot 建立時間
 */
@Data
@Builder
@Jacksonized
public class MatchingEngineSnapshot {

    private final String symbolCode;

    private final long matchSequence;

    private final long commandOffset;

    private final long eventOffset;

    private final List<Order> bids;

    private final List<Order> asks;

    private final Instant createdAt;
    public MatchingEngineSnapshot(String symbolCode, long matchSequence, long commandOffset, long eventOffset, List<Order> bids, List<Order> asks, Instant createdAt) {
        this.symbolCode = symbolCode;
        this.matchSequence = matchSequence;
        this.commandOffset = commandOffset;
        this.eventOffset = eventOffset;
        this.bids = bids;
        this.asks = asks;
        this.createdAt = createdAt;
    }

    public String symbolCode() {
        return symbolCode;
    }

    public long matchSequence() {
        return matchSequence;
    }

    public long commandOffset() {
        return commandOffset;
    }

    public long eventOffset() {
        return eventOffset;
    }

    public List<Order> bids() {
        return bids;
    }

    public List<Order> asks() {
        return asks;
    }

    public Instant createdAt() {
        return createdAt;
    }
}