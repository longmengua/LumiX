package com.lumix.ledger.persistence;

import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerJournalDraft;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 預設的 ledger append-only mapping 實作。
 *
 * 這個實作只負責把 domain draft 轉成 persistence mapping，不會接任何 repository 或 database client。
 */
public class DefaultLedgerAppendOnlyPersistenceMapper implements LedgerAppendOnlyPersistencePort {

    /**
     * 依照 append-only contract 產生 mapping plan。
     *
     * 這裡只做欄位對應，不推導時間、不呼叫資料庫，也不處理任何資金異動。
     */
    @Override
    public LedgerAppendOnlyPersistenceMapping describeAppendOnlyMapping(LedgerJournalDraft journalDraft, Instant postedAt) {
        // 這裡只做欄位對應，不做儲存，也不做任何資金異動。
        Objects.requireNonNull(journalDraft, "journalDraft must not be null");
        Objects.requireNonNull(postedAt, "postedAt must not be null");

        LedgerJournalPersistenceMapping journal = new LedgerJournalPersistenceMapping(
                journalDraft.businessReferenceType(),
                journalDraft.businessReferenceId(),
                null,
                null,
                postedAt
        );

        List<LedgerEntryPersistenceMapping> entries = journalDraft.entries().stream()
                .map(this::mapEntry)
                .toList();

        return new LedgerAppendOnlyPersistenceMapping(journal, entries);
    }

    private LedgerEntryPersistenceMapping mapEntry(LedgerEntryDraft entryDraft) {
        // entry 先保留可解析的 journal reference，避免這個 phase 誤以為已完成真正寫入。
        return new LedgerEntryPersistenceMapping(
                java.util.Optional.empty(),
                entryDraft.entrySequence(),
                entryDraft.accountId(),
                entryDraft.assetSymbol(),
                entryDraft.direction(),
                entryDraft.amount()
        );
    }
}
