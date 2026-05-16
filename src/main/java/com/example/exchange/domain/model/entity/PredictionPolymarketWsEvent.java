package com.example.exchange.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "prediction_polymarket_ws_event",
        indexes = {
                @Index(name = "idx_poly_ws_event_key", columnList = "event_key"),
                @Index(name = "idx_poly_ws_order", columnList = "order_id"),
                @Index(name = "idx_poly_ws_trade", columnList = "trade_id"),
                @Index(name = "idx_poly_ws_type", columnList = "event_type")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_poly_ws_event_key",
                        columnNames = "event_key"
                )
        }
)
public class PredictionPolymarketWsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, length = 256)
    private String eventKey;

    @Column(name = "event_type", length = 64)
    private String eventType;

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "wallet_address", length = 64)
    private String walletAddress;

    @Column(name = "market", length = 128)
    private String market;

    @Column(name = "asset_id", length = 256)
    private String assetId;

    @Column(name = "order_id", length = 128)
    private String orderId;

    @Column(name = "trade_id", length = 128)
    private String tradeId;

    @Lob
    @Column(name = "payload", columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
