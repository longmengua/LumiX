/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class RecoveryResult {

    private final long uid;

    private final boolean recovered;

    private final long snapshotSeq;

    private final long replayFromSeq;

    private final int replayedEvents;

    private final Instant recoveredAt;
    public RecoveryResult(long uid, boolean recovered, long snapshotSeq, long replayFromSeq, int replayedEvents, Instant recoveredAt) {
        this.uid = uid;
        this.recovered = recovered;
        this.snapshotSeq = snapshotSeq;
        this.replayFromSeq = replayFromSeq;
        this.replayedEvents = replayedEvents;
        this.recoveredAt = recoveredAt;
    }

    public long uid() {
        return uid;
    }

    public boolean recovered() {
        return recovered;
    }

    public long snapshotSeq() {
        return snapshotSeq;
    }

    public long replayFromSeq() {
        return replayFromSeq;
    }

    public int replayedEvents() {
        return replayedEvents;
    }

    public Instant recoveredAt() {
        return recoveredAt;
    }
}