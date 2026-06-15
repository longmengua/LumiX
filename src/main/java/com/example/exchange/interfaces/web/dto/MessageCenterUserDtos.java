/*
 * File purpose: 訊息中心前後端契約（使用者 API）。
 */
package com.example.exchange.interfaces.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class MessageCenterUserDtos {

    private MessageCenterUserDtos() {
    }

    public record MessageListItem(
            String messageId,
            String title,
            String summary,
            String category,
            String severity,
            Instant createdAt,
            boolean isRead,
            boolean isPinned,
            boolean isDeleted,
            boolean isArchived,
            boolean isExpired,
            boolean isScheduled,
            String actionUrl,
            String actionLabel
    ) {
    }

    public record MessageListResponse(
            List<MessageListItem> items,
            String nextCursor,
            boolean hasMore
    ) {
    }

    public record MessageDetailResponse(
            String messageId,
            String title,
            String summary,
            String body,
            String category,
            String severity,
            Instant createdAt,
            Instant effectiveAt,
            Instant expireAt,
            boolean isRead,
            Instant readAt,
            boolean isDeleted,
            boolean isArchived,
            boolean isPinned,
            boolean isExpired,
            boolean isScheduled,
            String actionUrl,
            String actionLabel,
            Map<String, Object> metadata
    ) {
    }

    public record MessageActionResponse(
            String messageId,
            Boolean isRead,
            Instant readAt,
            Boolean isDeleted,
            Boolean isArchived,
            Boolean isPinned
    ) {
    }

    public record MessageReadAllRequest(
            String scope,
            String category
    ) {
    }

    public record MessageReadAllResponse(
            int updatedCount
    ) {
    }

    public record MessageUnreadCountResponse(
            long unreadCount,
            Map<String, Long> byCategory
    ) {
    }

    public record MessagePreferenceItem(
            String category,
            boolean inAppEnabled,
            boolean emailEnabled,
            boolean smsEnabled,
            boolean pushEnabled,
            boolean locked
    ) {
    }

    public record MessagePreferencesResponse(
            List<MessagePreferenceItem> preferences
    ) {
    }

    public record MessagePreferenceUpdateRequest(
            List<MessagePreferenceItem> preferences
    ) {
    }

    public record MessagePreferenceUpdateResponse(
            int updated,
            List<MessagePreferenceItem> preferences
    ) {
    }

    public record MessageSendOutcome(
            String messageId,
            String title,
            String category,
            String severity,
            long sentTo,
            long skipped,
            boolean alreadyExists,
            Instant sendAt
    ) {
    }

    public record MessageSystemSendRequest(
            String title,
            String summary,
            String body,
            String category,
            String severity,
            String templateCode,
            Map<String, Object> templateVars,
            Map<String, Object> metadata,
            String sourceEventType,
            String sourceEventId,
            String sourceEventHash,
            String dedupeKey,
            String actionUrl,
            String actionLabel,
            Instant effectiveAt,
            Instant expireAt,
            Instant sendAt,
            List<Long> recipientUserIds,
            String audienceType,
            Map<String, Object> audienceData
    ) {
    }

    public record MessageAnnouncementCreateRequest(
            String title,
            String summary,
            String category,
            String severity,
            String templateCode,
            Map<String, Object> templateVars,
            String actionUrl,
            String actionLabel,
            Instant sendAt,
            Instant expireAt,
            String audienceType,
            List<Long> recipientUserIds,
            Map<String, Object> audienceData,
            String deliveryMode,
            String dedupeKey
    ) {
    }

    public record MessageAnnouncementCreateResponse(
            String announcementId,
            String status,
            long estimatedRecipients,
            MessageSendOutcome result
    ) {
    }

    public record MessageAnnouncementCancelResponse(
            String announcementId,
            boolean canceled
    ) {
    }
}
