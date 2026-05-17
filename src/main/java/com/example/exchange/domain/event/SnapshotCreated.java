/*
 * 檔案用途：領域事件，描述交易、快照、資金費或強平等已發生的業務事實。
 */
package com.example.exchange.domain.event;

import java.time.Instant;

/**
 * 快照建立事件（可用於審計或觀測）
 *
 * - lastSeq: 當下快照覆蓋到的最後事件序號
 */
public record SnapshotCreated(
        long uid,
        long lastSeq,
        Instant ts
) {}
