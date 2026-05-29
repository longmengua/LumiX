/*
 * 檔案用途：JPA entity，保存 hedge venue submit idempotency claim 與 terminal result。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "hedge_venue_idempotency_record",
        indexes = {
                @Index(name = "idx_hedge_venue_idem_completed", columnList = "completed")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hedge_venue_idem_ref_id",
                        columnNames = "ref_id"
                )
        }
)
public class HedgeVenueIdempotencyRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ref_id", nullable = false, length = 128)
    private String refId;

    @Column(name = "fingerprint", nullable = false, length = 512)
    private String fingerprint;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "accepted")
    private Boolean accepted;

    @Column(name = "venue_order_id", length = 128)
    private String venueOrderId;

    @Column(name = "reason", length = 256)
    private String reason;

    @Column(name = "retryable")
    private Boolean retryable;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
