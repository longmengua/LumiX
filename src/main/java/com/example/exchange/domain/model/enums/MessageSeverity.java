/*
 * File purpose: 訊息中心嚴重性枚舉。
 */
package com.example.exchange.domain.model.enums;

public enum MessageSeverity {
    INFO,
    SUCCESS,
    WARNING,
    CRITICAL

    ;

    public static MessageSeverity parse(String value) {
        if (value == null) {
            return INFO;
        }
        return MessageSeverity.valueOf(value.trim().toUpperCase());
    }
}
