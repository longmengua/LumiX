/*
 * 檔案用途：JPA entity，保存 production durable dead-letter event。
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
        name = "dlq_events",
        indexes = {
                @Index(name = "idx_dlq_created_at", columnList = "created_at"),
                @Index(name = "idx_dlq_topic_key", columnList = "topic,event_key")
        }
)
public class DlqEventRecord {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "outbox_id", length = 36)
    private String outboxId;

    @Column(name = "topic", nullable = false, length = 128)
    private String topic;

    @Column(name = "event_key", nullable = false, length = 256)
    private String eventKey;

    @Column(name = "event_type", nullable = false, length = 256)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant createdAt;
}
