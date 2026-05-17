/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

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
