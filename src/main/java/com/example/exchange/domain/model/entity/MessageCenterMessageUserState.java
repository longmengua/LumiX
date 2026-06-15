/*
 * File purpose: 訊息中心使用者狀態主檔（已讀/刪除/封存/釘選）。
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
@IdClass(MessageCenterMessageUserState.MessageStateId.class)
@Table(
        name = "message_center_message_states",
        indexes = {
                @Index(name = "idx_msg_state_uid", columnList = "uid"),
                @Index(name = "idx_msg_state_uid_dedupe", columnList = "uid,dedupe_key"),
                @Index(name = "idx_msg_state_uid_archived", columnList = "uid,is_archived")
        }
)
public class MessageCenterMessageUserState {

    @Id
    @Column(name = "uid", nullable = false)
    private long uid;

    @Id
    @Column(name = "message_id", nullable = false, length = 36)
    private String messageId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at", columnDefinition = "TIMESTAMP(6)")
    private Instant readAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    @Column(name = "pin_at", columnDefinition = "TIMESTAMP(6)")
    private Instant pinAt;

    @Column(name = "last_notified_at", columnDefinition = "TIMESTAMP(6)")
    private Instant lastNotifiedAt;

    @Column(name = "dedupe_key", length = 255)
    private String dedupeKey;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
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

    /**
     * Projection 以 getIsRead 命名存取時可直接對應，避免與 @Getter 產生的方法衝突。
     */
    public boolean getIsRead() {
        return read;
    }

    /**
     * Projection 以 getIsDeleted 命名時可直接對應。
     */
    public boolean getIsDeleted() {
        return deleted;
    }

    /**
     * Projection 以 getIsArchived 命名時可直接對應。
     */
    public boolean getIsArchived() {
        return archived;
    }

    /**
     * Projection 以 getIsPinned 命名時可直接對應。
     */
    public boolean getIsPinned() {
        return pinned;
    }

    public void markRead(Instant when) {
        if (!read) {
            read = true;
            readAt = when;
        }
    }

    public void setPinned(boolean pinned, Instant at) {
        this.pinned = pinned;
        this.pinAt = pinned ? at : null;
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    public static class MessageStateId implements Serializable {
        private long uid;
        private String messageId;
    }
}
