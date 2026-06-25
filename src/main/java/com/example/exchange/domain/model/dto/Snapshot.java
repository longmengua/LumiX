/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * Snapshot (快照)
 *
 * - 保存某個時間點的帳戶/倉位/訂單資料
 * - lastEventSeq: 用於恢復時 replay event
 */
@Data
@Builder
@Jacksonized
public class Snapshot {

    private final long uid;

    private final Map<String, Object> aggregates;

    private final Instant createdAt;

    private final long lastEventSeq;
    public Snapshot(long uid, Map<String, Object> aggregates, Instant createdAt, long lastEventSeq) {
        this.uid = uid;
        this.aggregates = aggregates;
        this.createdAt = createdAt;
        this.lastEventSeq = lastEventSeq;
    }

    public long uid() {
        return uid;
    }

    public Map<String, Object> aggregates() {
        return aggregates;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public long lastEventSeq() {
        return lastEventSeq;
    }
}