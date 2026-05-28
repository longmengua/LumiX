/*
 * 檔案用途：領域 DTO，承載 per-symbol matching sequencer ownership lease。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;

/**
 * Matching sequencer lease。
 *
 * @param symbolCode          normalized symbol code
 * @param ownerId             worker / process owner id
 * @param epoch               fencing epoch；新 owner 接手時必須遞增
 * @param expiresAt           lease 到期時間
 * @param commandOffset       owner 觀察到的最後 command offset
 * @param eventOffset         owner 觀察到的最後 event offset
 * @param updatedAt           lease 更新時間
 */
public record MatchingSequencerLease(
        String symbolCode,
        String ownerId,
        long epoch,
        Instant expiresAt,
        long commandOffset,
        long eventOffset,
        Instant updatedAt
) {
}
