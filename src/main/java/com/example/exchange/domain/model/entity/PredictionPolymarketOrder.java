package com.example.exchange.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "prediction_polymarket_order",
        indexes = {
                @Index(name = "idx_poly_order_internal", columnList = "internal_order_id"),
                @Index(name = "idx_poly_order_clob", columnList = "clob_order_id"),
                @Index(name = "idx_poly_order_status", columnList = "status"),
                @Index(name = "idx_poly_order_market", columnList = "market_slug"),
                @Index(name = "idx_poly_order_session", columnList = "session_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_poly_internal_order_id",
                        columnNames = "internal_order_id"
                )
        }
)
public class PredictionPolymarketOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "internal_order_id", nullable = false, length = 64)
    private String internalOrderId;

    @Column(name = "clob_order_id", length = 128)
    private String clobOrderId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "event_slug", length = 128)
    private String eventSlug;

    @Column(name = "market_slug", nullable = false, length = 256)
    private String marketSlug;

    @Column(name = "condition_id", length = 128)
    private String conditionId;

    @Column(name = "outcome_key", length = 32)
    private String outcomeKey;

    @Column(name = "token_id", length = 256)
    private String tokenId;

    @Column(name = "direction", length = 32)
    private String direction;

    @Column(name = "side", length = 16)
    private String side;

    @Column(name = "order_type", length = 16)
    private String orderType;

    @Column(name = "price", precision = 38, scale = 18)
    private BigDecimal price;

    @Column(name = "size", precision = 38, scale = 18)
    private BigDecimal size;

    @Column(name = "usdt_amount", precision = 38, scale = 18)
    private BigDecimal usdtAmount;

    @Column(name = "status", nullable = false, length = 64)
    private String status;

    @Column(name = "trade_status", length = 64)
    private String tradeStatus;

    @Column(name = "size_matched", precision = 38, scale = 18)
    private BigDecimal sizeMatched;

    @Column(name = "last_trade_id", length = 128)
    private String lastTradeId;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Lob
    @Column(name = "last_clob_payload", columnDefinition = "LONGTEXT")
    private String lastClobPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

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
