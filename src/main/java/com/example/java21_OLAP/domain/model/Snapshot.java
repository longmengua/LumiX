package com.example.java21_OLAP.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Snapshot (快照)
 *
 * - 保存某個時間點的帳戶/倉位/訂單資料
 * - lastEventSeq: 用於恢復時 replay event
 */
public record Snapshot(
        long uid,
        Map<String, Object> aggregates,
        Instant createdAt,
        long lastEventSeq
) {}
