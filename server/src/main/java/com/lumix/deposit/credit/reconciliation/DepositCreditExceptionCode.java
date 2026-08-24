package com.lumix.deposit.credit.reconciliation;

/** 唯讀 reconciliation 偵測到的 fail-closed exception 類別。 */
public enum DepositCreditExceptionCode {
    MISSING_LEDGER_EVIDENCE,
    MISSING_BALANCE_EVIDENCE,
    OWNER_OR_ASSET_MISMATCH,
    ATOMIC_AMOUNT_MISMATCH,
    INELIGIBLE_RECORD_HAS_POSTING_EVIDENCE
}
