package com.lumix.ledger.persistence;

import java.util.List;
import java.util.Objects;

/**
 * ledger append-only persistence mapping plan。
 *
 * 這份 plan 只描述 journal header 與 entry rows 的 mapping 形狀，不做任何資料庫操作。
 */
public record LedgerAppendOnlyPersistenceMapping(
        LedgerJournalPersistenceMapping journal,
        List<LedgerEntryPersistenceMapping> entries
) {

    public LedgerAppendOnlyPersistenceMapping {
        // append-only 只接受完整且可重建的 mapping 結果，避免將空 plan 誤判為成功。
        Objects.requireNonNull(journal, "journal must not be null");
        Objects.requireNonNull(entries, "entries must not be null");
        entries = List.copyOf(entries);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("entries must not be empty");
        }
    }
}
