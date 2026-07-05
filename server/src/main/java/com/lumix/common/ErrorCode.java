package com.lumix.common;

/**
 * Phase 9 的基礎錯誤碼集合。
 * 先保留最小可用集合，避免把 production 等級錯誤體系提早做過頭。
 */
public enum ErrorCode {
    INVALID_REQUEST,
    INVALID_AMOUNT,
    UNSUPPORTED_ACCOUNT_TYPE,
    SAME_ACCOUNT_TRANSFER_NOT_ALLOWED,
    NOT_IMPLEMENTED
}
