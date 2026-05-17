/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;

public record RecoveryResult(
        long uid,
        boolean recovered,
        long snapshotSeq,
        long replayFromSeq,
        int replayedEvents,
        Instant recoveredAt
) {}
