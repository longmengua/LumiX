/*
 * File purpose: 訊息偏好設定（每位使用者每分類）。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@Entity
@IdClass(MessageCenterNotificationPreference.PreferenceId.class)
@Table(
        name = "message_center_notification_preferences",
        indexes = {
                @Index(name = "idx_msg_pref_uid_category", columnList = "uid,category")
        }
)
public class MessageCenterNotificationPreference {

    @Id
    @Column(name = "uid", nullable = false)
    private long uid;

    @Id
    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    void prePersist() {
        updatedAt = Instant.now();
        inAppEnabled = true;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public boolean lockedChannels() {
        return "SECURITY".equalsIgnoreCase(category) || "COMPLIANCE".equalsIgnoreCase(category);
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    public static class PreferenceId implements Serializable {
        private long uid;
        private String category;
    }
}
