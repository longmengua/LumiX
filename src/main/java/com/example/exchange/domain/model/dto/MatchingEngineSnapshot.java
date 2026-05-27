/*
 * 檔案用途：領域 DTO，承載 matching engine 可恢復狀態快照。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.entity.Order;

import java.time.Instant;
import java.util.List;

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
public record MatchingEngineSnapshot(
        String symbolCode,
        long matchSequence,
        long commandOffset,
        long eventOffset,
        List<Order> bids,
        List<Order> asks,
        Instant createdAt
) {
}
