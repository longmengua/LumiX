package com.lumix.ledger.application;

/**
 * ledger runtime 啟動前的必要 prerequisite。
 *
 * 這些項目只描述 boundary 與資料前提，不代表 posting、reconciliation 或 mutation 已經完成。
 */
public enum LedgerRuntimePrerequisite {
    IDENTITY_BOUNDARY,
    ACCOUNT_BOUNDARY,
    ASSET_BOUNDARY,
    MARKET_BOUNDARY,
    LEDGER_JOURNAL_SCHEMA,
    LEDGER_ENTRY_SCHEMA,
    BALANCE_PROJECTION_READ_MODEL,
    APPEND_ONLY_GOVERNANCE
}
