package com.lumix.ledger.application.transaction;

import com.lumix.ledger.application.posting.LedgerPostingPlan;

/**
 * ledger append transaction design policy。
 *
 * 這個 policy 只整理 transaction 步驟與安全註記，不會做任何 runtime mutation。
 */
public class LedgerAppendTransactionPolicy implements LedgerAppendTransactionBoundary {

    @Override
    public LedgerAppendTransactionDesign describe(LedgerPostingPlan postingPlan) {
        // 這裡只輸出設計，不碰 DB、不開 transaction，也不觸發任何 side effect。
        java.util.Objects.requireNonNull(postingPlan, "postingPlan must not be null");

        return new LedgerAppendTransactionDesign(
                postingPlan.command().requestId(),
                postingPlan,
                java.util.List.of(
                        LedgerAppendTransactionStep.IDEMPOTENCY_CHECK_OR_LOCK,
                        LedgerAppendTransactionStep.JOURNAL_HEADER_APPEND,
                        LedgerAppendTransactionStep.JOURNAL_ENTRIES_APPEND,
                        LedgerAppendTransactionStep.OUTBOX_APPEND,
                        LedgerAppendTransactionStep.AUDIT_APPEND,
                        LedgerAppendTransactionStep.COMMIT
                ),
                java.util.List.of(
                        "ledger append 必須是單一 transaction",
                        "requestId 只負責 trace / correlation / audit linkage；idempotency key 才負責 duplicate prevention contract",
                        "projection read model 不是 source of truth，不能在此 transaction 內直接更新",
                        "交易隔離、locking、retry、deadlock handling 只作設計記錄，不在本階段實作",
                        "所有正式 append runtime 都屬於 HUMAN_REVIEW_REQUIRED"
                )
        );
    }
}
