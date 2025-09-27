package com.example.java21_OLAP.domain.event;

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
