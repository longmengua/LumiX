package com.lumix.account;

/**
 * 帳戶狀態。
 * 僅保留最小狀態集合，作為後續查詢與風控規則的讀取基礎。
 */
public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED
}
