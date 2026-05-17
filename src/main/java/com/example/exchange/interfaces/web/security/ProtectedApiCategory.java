/*
 * 檔案用途：Web 安全模型，分類受保護 API 以套用不同授權規則。
 */
package com.example.exchange.interfaces.web.security;

public enum ProtectedApiCategory {
    TRADING,
    FUNDS,
    ADMIN,
    PROTECTED,
    UNKNOWN
}
