/*
 * File purpose: 訊息中心訊息主檔。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "message_center_messages",
        indexes = {
                @Index(name = "idx_msg_center_msg_created_id", columnList = "created_at,id"),
                @Index(name = "idx_msg_center_msg_category_created", columnList = "category,created_at"),
                @Index(name = "idx_msg_center_msg_expire", columnList = "expire_at")
        }
)
public class MessageCenterMessage {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "template_code", nullable = false, length = 128)
    private String templateCode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "body", nullable = false, columnDefinition = "LONGTEXT")
    private String body;

    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "severity", nullable = false, length = 32)
    private String severity;

    @Column(name = "action_url", length = 512)
    private String actionUrl;

    @Column(name = "action_label", length = 128)
    private String actionLabel;

    @Column(name = "metadata_json", nullable = false, columnDefinition = "LONGTEXT")
    private String metadataJson;

    @Column(name = "template_vars_json", nullable = false, columnDefinition = "LONGTEXT")
    private String templateVarsJson;

    @Column(name = "source_user_id")
    private Long sourceUserId;

    @Column(name = "source_event_type", length = 128)
    private String sourceEventType;

    @Column(name = "source_event_id", length = 128)
    private String sourceEventId;

    @Column(name = "source_event_hash", length = 128)
    private String sourceEventHash;

    @Column(name = "dedupe_key", length = 255)
    private String dedupeKey;

    @Column(name = "created_by_subject", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_by_type", nullable = false, length = 32)
    private String createdByType;

    @Column(name = "effective_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant effectiveAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "expire_at", columnDefinition = "TIMESTAMP(6)")
    private Instant expireAt;

    @Column(name = "is_scheduled", nullable = false)
    private boolean scheduled;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (effectiveAt == null) {
            effectiveAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (metadataJson == null) {
            metadataJson = "{}";
        }
        if (templateVarsJson == null) {
            templateVarsJson = "{}";
        }
        if (templateCode == null) {
            templateCode = "";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isExpired(Instant now) {
        return expireAt != null && !expireAt.isAfter(now == null ? Instant.now() : now);
    }
}
