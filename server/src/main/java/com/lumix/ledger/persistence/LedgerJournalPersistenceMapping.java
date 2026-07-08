package com.lumix.ledger.persistence;

import com.lumix.ledger.domain.LedgerBusinessReferenceType;

import java.time.Instant;
import java.util.Objects;

/**
 * ledger_journals 的欄位 mapping contract。
 *
 * 這份 mapping 只描述 Phase 12 journal header 的語意，不代表已經完成寫入。
 */
public record LedgerJournalPersistenceMapping(
        LedgerBusinessReferenceType businessReferenceType,
        String businessReferenceId,
        String requestId,
        String journalNote,
        Instant postedAt
) {

    public LedgerJournalPersistenceMapping {
        // journal header 只保留 append-only 需要的欄位，避免把 runtime 狀態混進 mapping。
        Objects.requireNonNull(businessReferenceType, "businessReferenceType must not be null");
        requireText(businessReferenceId, "businessReferenceId");
        if (requestId != null) {
            requestId = requestId.trim();
            if (requestId.isEmpty()) {
                throw new IllegalArgumentException("requestId must not be blank when provided");
            }
        }
        if (journalNote != null) {
            journalNote = journalNote.trim();
            if (journalNote.isEmpty()) {
                throw new IllegalArgumentException("journalNote must not be blank when provided");
            }
        }
        Objects.requireNonNull(postedAt, "postedAt must not be null");
    }

    private static void requireText(String value, String fieldName) {
        // reference 欄位會被用於對帳與稽核，空白值會讓 journal header 失去可追蹤性。
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
