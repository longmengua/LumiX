package com.lumix.ledger;

import com.lumix.common.RequestId;

import java.util.List;
import java.util.Objects;

/**
 * 帳本 journal 請求。
 * 用於描述一組分錄，但 Phase 9 不會落地任何真實帳務引擎。
 */
public record LedgerJournalRequest(
        RequestId requestId,
        String businessType,
        String reason,
        String referenceId,
        List<LedgerPostingRequest> postings
) {

    public LedgerJournalRequest {
        // journal 是高風險操作，核心識別欄位必須完整。
        Objects.requireNonNull(requestId, "requestId must not be null");
        businessType = requireText(businessType, "businessType");
        reason = requireText(reason, "reason");
        referenceId = requireText(referenceId, "referenceId");
        // 分錄集合不可為空，否則無法表達任何實際帳務事件。
        Objects.requireNonNull(postings, "postings must not be null");
        postings = List.copyOf(postings);
        if (postings.isEmpty()) {
            throw new IllegalArgumentException("postings must not be empty");
        }
    }

    private static String requireText(String value, String fieldName) {
        // 字串欄位要先做非空白正規化，避免下游儲存出現垃圾值。
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
