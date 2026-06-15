/*
 * File purpose: 公告目標受眾枚舉。
 */
package com.example.exchange.domain.model.enums;

public enum MessageAudienceType {
    ALL,
    USER_IDS,
    VIP,
    HAS_ASSET,
    CUSTOM_FILTER

    ;

    public static MessageAudienceType parse(String value) {
        if (value == null) {
            return ALL;
        }
        return MessageAudienceType.valueOf(value.trim().toUpperCase());
    }
}
