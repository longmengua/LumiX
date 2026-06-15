/*
 * File purpose: 後台公告定義與發送作業記錄。
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
        name = "message_center_announcements",
        indexes = {
                @Index(name = "idx_msg_announce_status_send_at", columnList = "status,send_at"),
                @Index(name = "idx_msg_announce_category_status", columnList = "category,status"),
                @Index(name = "idx_msg_announce_created_at", columnList = "created_at")
        }
)
public class MessageCenterAnnouncement {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "severity", nullable = false, length = 32)
    private String severity;

    @Column(name = "template_code", length = 128)
    private String templateCode;

    @Column(name = "template_vars_json", nullable = false, columnDefinition = "LONGTEXT")
    private String templateVarsJson;

    @Column(name = "action_url", length = 512)
    private String actionUrl;

    @Column(name = "action_label", length = 128)
    private String actionLabel;

    @Column(name = "audience_type", nullable = false, length = 32)
    private String audienceType;

    @Column(name = "audience_data", nullable = false, columnDefinition = "LONGTEXT")
    private String audienceData;

    @Column(name = "send_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant sendAt;

    @Column(name = "expire_at", columnDefinition = "TIMESTAMP(6)")
    private Instant expireAt;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "delivery_mode", nullable = false, length = 32)
    private String deliveryMode;

    @Column(name = "dedupe_key", length = 255)
    private String dedupeKey;

    @Column(name = "estimated_recipients", nullable = false)
    private long estimatedRecipients;

    @Column(name = "sent_count", nullable = false)
    private long sentCount;

    @Column(name = "failed_count", nullable = false)
    private long failedCount;

    @Column(name = "skipped_count", nullable = false)
    private long skippedCount;

    @Column(name = "created_by_subject", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_by_type", nullable = false, length = 32)
    private String createdByType;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (templateVarsJson == null) {
            templateVarsJson = "{}";
        }
        if (audienceData == null) {
            audienceData = "{}";
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
