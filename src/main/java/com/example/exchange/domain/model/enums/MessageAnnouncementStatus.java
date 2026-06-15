/*
 * File purpose: 公告生命週期狀態枚舉。
 */
package com.example.exchange.domain.model.enums;

public enum MessageAnnouncementStatus {
    DRAFT,
    SCHEDULED,
    PUBLISHED,
    CANCELLED

    ;

    public static MessageAnnouncementStatus parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("announcement status is required");
        }
        return MessageAnnouncementStatus.valueOf(value.trim().toUpperCase());
    }
}
