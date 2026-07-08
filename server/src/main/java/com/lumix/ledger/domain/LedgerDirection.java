package com.lumix.ledger.domain;

/**
 * ledger 分錄方向。
 *
 * 只定義 DEBIT / CREDIT 兩種方向，避免在 domain 層混入金額正負號語意。
 */
public enum LedgerDirection {
    DEBIT,
    CREDIT
}
