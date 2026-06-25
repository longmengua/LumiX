/*
 * 檔案用途：領域 DTO，承載 per-symbol matching sequencer ownership lease。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


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
@Data
@Builder
@Jacksonized
public class MatchingSequencerLease {

    private final String symbolCode;

    private final String ownerId;

    private final long epoch;

    private final Instant expiresAt;

    private final long commandOffset;

    private final long eventOffset;

    private final Instant updatedAt;
    public MatchingSequencerLease(String symbolCode, String ownerId, long epoch, Instant expiresAt, long commandOffset, long eventOffset, Instant updatedAt) {
        this.symbolCode = symbolCode;
        this.ownerId = ownerId;
        this.epoch = epoch;
        this.expiresAt = expiresAt;
        this.commandOffset = commandOffset;
        this.eventOffset = eventOffset;
        this.updatedAt = updatedAt;
    }

    public String symbolCode() {
        return symbolCode;
    }

    public String ownerId() {
        return ownerId;
    }

    public long epoch() {
        return epoch;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public long commandOffset() {
        return commandOffset;
    }

    public long eventOffset() {
        return eventOffset;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}