package com.lumix.ledger.runtime;

/**
 * 內部 ledger 入帳結果。
 *
 * <p>成功只代表 journal、entries、idempotency、outbox 與 audit 已原子落庫；它不代表餘額投影、
 * reservation、結算、入金或提款完成。</p>
 */
public record LedgerPostingExecutionResult(long ledgerJournalId, boolean replayed) {
}
