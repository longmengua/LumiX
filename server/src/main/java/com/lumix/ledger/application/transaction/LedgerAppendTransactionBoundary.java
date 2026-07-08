package com.lumix.ledger.application.transaction;

import com.lumix.ledger.application.posting.LedgerPostingPlan;

/**
 * ledger append transaction boundary marker。
 *
 * 這個 boundary 只描述 transaction design，不執行 repository、JDBC 或任何資料庫 client 呼叫。
 */
public interface LedgerAppendTransactionBoundary {

    /**
     * 依照 posting plan 產生 transaction design。
     *
     * 這個方法只回傳設計資料，不會開始 transaction，也不會寫入 ledger_journals / ledger_entries。
     */
    LedgerAppendTransactionDesign describe(LedgerPostingPlan postingPlan);
}
