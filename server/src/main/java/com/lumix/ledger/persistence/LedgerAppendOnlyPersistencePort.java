package com.lumix.ledger.persistence;

import com.lumix.ledger.domain.LedgerJournalDraft;

import java.time.Instant;

/**
 * ledger persistence 的 append-only 契約入口。
 *
 * 這個 port 只描述 mapping contract，不提供真正 repository 寫入；
 * 任何正式持久化都必須再經過 HUMAN_REVIEW_REQUIRED。
 */
public interface LedgerAppendOnlyPersistencePort {

    /**
     * 將 journal draft 轉成 append-only persistence mapping。
     *
     * 這個方法只回傳 mapping contract，不會觸發 DB write，也不會更新任何 read model。
     */
    LedgerAppendOnlyPersistenceMapping describeAppendOnlyMapping(LedgerJournalDraft journalDraft, Instant postedAt);
}
