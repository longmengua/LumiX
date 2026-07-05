package com.lumix.account;

/**
 * 帳戶類型。
 * Phase 9 只允許 SPOT / FUTURES / MARGIN，避免把不同資產域混成通用帳戶。
 */
public enum AccountType {
    SPOT,
    FUTURES,
    MARGIN
}
