package com.lumix.ledger.application.idempotency;

import java.util.List;
import java.util.Objects;

/**
 * ledger idempotency design 的輸出契約。
 *
 * 這份契約只用來描述 request identity 與 decision 狀態，不代表 runtime 已完成查詢、鎖定或重試。
 */
public record LedgerIdempotencyDesign(
        LedgerRequestIdentityContract requestIdentity,
        LedgerIdempotencyDecision decision,
        List<String> notes
) {

    public LedgerIdempotencyDesign {
        // 設計輸出必須可審核與可重建，因此避免把可變集合直接暴露出去。
        Objects.requireNonNull(requestIdentity, "requestIdentity must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(notes, "notes must not be null");
        notes = List.copyOf(notes);
    }
}
