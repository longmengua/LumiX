package com.lumix.ledger.persistence;

import java.util.List;
import java.util.Objects;

/**
 * ledger append-only persistence mapping。
 *
 * <p>這個型別只描述未來要 append 到 ledger_journals 與 ledger_entries 的資料形狀，
 * 不代表已執行 DB 寫入，也不代表正式 posting runtime 已完成。</p>
 *
 * <p>HUMAN_REVIEW_REQUIRED：任何把此 mapping 接到正式資金路徑的變更，都必須人工審核。</p>
 */
public record LedgerAppendOnlyPersistenceMapping(
        LedgerJournalPersistenceMapping journal,
        List<LedgerEntryPersistenceMapping> entries
) {

    public LedgerAppendOnlyPersistenceMapping {
        journal = Objects.requireNonNull(journal, "journal must not be null");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries must not be null"));
    }
}
