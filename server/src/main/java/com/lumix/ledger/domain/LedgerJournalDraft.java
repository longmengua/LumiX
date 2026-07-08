package com.lumix.ledger.domain;

import java.util.List;
import java.util.Objects;

/**
 * ledger journal draft。
 *
 * 這份 draft 只是一個 posting 前的 domain contract，用來讓 invariant policy 檢查
 * journal 是否具備可接受的雙邊平衡結構。
 */
public record LedgerJournalDraft(
        LedgerBusinessReferenceType businessReferenceType,
        String businessReferenceId,
        List<LedgerEntryDraft> entries
) {

    public LedgerJournalDraft {
        // journal draft 先保留 business reference，方便未來對照 Phase 12 的 journal header。
        Objects.requireNonNull(businessReferenceType, "businessReferenceType must not be null");
        businessReferenceId = requireText(businessReferenceId, "businessReferenceId");
        Objects.requireNonNull(entries, "entries must not be null");
        entries = List.copyOf(entries);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("entries must not be empty");
        }
    }

    private static String requireText(String value, String fieldName) {
        // referenceId 會被拿來做追蹤與稽核，空白值會讓後續對帳失真。
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
