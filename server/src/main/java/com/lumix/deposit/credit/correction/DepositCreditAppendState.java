package com.lumix.deposit.credit.correction;

/**
 * caller 已驗證的原 credit append 證據狀態；UNKNOWN 一律不得自動修正。
 */
public enum DepositCreditAppendState {
    NOT_APPENDED,
    CREDIT_APPEND_CONFIRMED,
    UNKNOWN
}
