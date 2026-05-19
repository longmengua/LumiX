/*
 * 檔案用途：JPA entity，保存 production durable outbox event。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_due", columnList = "status,next_attempt_at"),
                @Index(name = "idx_outbox_topic_key", columnList = "topic,event_key")
        }
)
public class OutboxEventRecord {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "topic", nullable = false, length = 128)
    private String topic;

    @Column(name = "event_key", nullable = false, length = 256)
    private String eventKey;

    @Column(name = "event_type", nullable = false, length = 256)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @Column(name = "headers", columnDefinition = "JSON")
    private String headers;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "next_attempt_at", columnDefinition = "TIMESTAMP(6)")
    private Instant nextAttemptAt;

    @Column(name = "published_at", columnDefinition = "TIMESTAMP(6)")
    private Instant publishedAt;
}
