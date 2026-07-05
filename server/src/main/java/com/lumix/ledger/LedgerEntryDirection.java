package com.lumix.ledger;

/**
 * 帳本分錄方向。
 * 只定義借 / 貸兩個方向，具體帳務計算不在 Phase 9 實作。
 */
public enum LedgerEntryDirection {
    DEBIT,
    CREDIT
}
