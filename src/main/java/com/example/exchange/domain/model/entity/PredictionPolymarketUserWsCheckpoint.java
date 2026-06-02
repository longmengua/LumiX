/*
 * 檔案用途：JPA checkpoint，保存 Polymarket user WebSocket gateway 已發布/重放的位置。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "prediction_polymarket_user_ws_checkpoint",
        indexes = {
                @Index(name = "idx_poly_user_ws_checkpoint_wallet", columnList = "wallet_address"),
                @Index(name = "idx_poly_user_ws_checkpoint_received", columnList = "last_received_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_poly_user_ws_checkpoint_stream",
                        columnNames = "stream_key"
                )
        }
)
public class PredictionPolymarketUserWsCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stream_key", nullable = false, length = 160)
    private String streamKey;

    @Column(name = "wallet_address", length = 64)
    private String walletAddress;

    @Column(name = "last_event_key", length = 256)
    private String lastEventKey;

    @Column(name = "last_event_type", length = 64)
    private String lastEventType;

    @Column(name = "last_received_at")
    private LocalDateTime lastReceivedAt;

    @Lob
    @Column(name = "last_payload", columnDefinition = "LONGTEXT")
    private String lastPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now =
                LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
