package com.lumix.ledger;

import com.lumix.common.RequestId;

import java.util.Objects;

/**
 * 帳本 journal 結果。
 * 目前只保留最小回傳資訊，方便未來接 API 與除錯。
 */
public record LedgerJournalResult(
        RequestId requestId,
        String status,
        String journalId,
        String message
) {

    public LedgerJournalResult {
        // requestId 與狀態是最基本的結果資訊，不能缺漏。
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
