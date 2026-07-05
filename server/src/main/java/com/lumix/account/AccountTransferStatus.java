package com.lumix.account;

/**
 * 帳戶劃轉狀態。
 * Phase 9 只保留最小語意，不假設已完成真實扣帳。
 */
public enum AccountTransferStatus {
    REJECTED,
    PENDING_LEDGER_REVIEW
}
