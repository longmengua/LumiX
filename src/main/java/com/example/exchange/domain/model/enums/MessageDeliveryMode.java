/*
 * File purpose: 公告發送模式枚舉。
 */
package com.example.exchange.domain.model.enums;

public enum MessageDeliveryMode {
    DIRECT,
    LAZY

    ;

    public static MessageDeliveryMode parse(String value) {
        if (value == null) {
            return LAZY;
        }
        return MessageDeliveryMode.valueOf(value.trim().toUpperCase());
    }
}
