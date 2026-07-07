package com.lumix.security;

/**
 * security policy 用的操作分類。
 *
 * 這些分類只表示安全風險等級，不代表 runtime 已經允許執行。
 */
public enum SecurityOperation {
    READ_ONLY_QUERY,
    WITHDRAWAL_REQUEST,
    LEDGER_POSTING,
    ADMIN_ACTION,
    SETTLEMENT,
    RISK_OVERRIDE
}
