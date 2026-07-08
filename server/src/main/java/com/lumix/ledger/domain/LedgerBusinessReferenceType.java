package com.lumix.ledger.domain;

/**
 * ledger journal 的 business reference 類型。
 *
 * 這份 enum 必須與 Phase 12 的 `ledger_journals.business_reference_type` 語意對齊，
 * 不得自行擴充成未審核的業務事件分類。
 */
public enum LedgerBusinessReferenceType {
    DEPOSIT,
    WITHDRAWAL,
    ORDER,
    TRADE,
    SETTLEMENT,
    FEE,
    ADJUSTMENT
}
