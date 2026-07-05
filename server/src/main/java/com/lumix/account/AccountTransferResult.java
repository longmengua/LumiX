package com.lumix.account;

import com.lumix.common.RequestId;

import java.util.Objects;

/**
 * 帳戶劃轉結果。
 * 目前僅用於回傳驗證與暫存狀態，不代表真實資產已完成移轉。
 */
public record AccountTransferResult(
        RequestId requestId,
        AccountTransferStatus status,
        String message,
        String ledgerJournalId
) {

    public AccountTransferResult {
        // 回應資料也屬於 API 合約的一部分，最少要保證識別碼、狀態與訊息存在。
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
