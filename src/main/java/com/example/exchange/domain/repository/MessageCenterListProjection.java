/*
 * File purpose: 訊息列表查詢投影。
 */
package com.example.exchange.domain.repository;

import java.time.Instant;

public interface MessageCenterListProjection {
    String getMessageId();

    String getTitle();

    String getSummary();

    String getBody();

    String getCategory();

    String getSeverity();

    Instant getCreatedAt();

    boolean getIsRead();

    boolean getIsDeleted();

    boolean getIsArchived();

    boolean getIsPinned();

    Instant getExpireAt();

    boolean getIsScheduled();

    String getActionUrl();

    String getActionLabel();
}
