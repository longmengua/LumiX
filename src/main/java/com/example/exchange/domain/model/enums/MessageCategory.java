/*
 * File purpose: 訊息中心分類枚舉。
 */
package com.example.exchange.domain.model.enums;

public enum MessageCategory {
    SYSTEM,
    ANNOUNCEMENT,
    ORDER,
    TRADE,
    DEPOSIT,
    WITHDRAW,
    ACCOUNT,
    SECURITY,
    PROMOTION,
    COMPLIANCE

    ;

    public static MessageCategory parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("message category is required");
        }
        return MessageCategory.valueOf(value.trim().toUpperCase());
    }

    public static boolean isKnown(String value) {
        try {
            parse(value);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
