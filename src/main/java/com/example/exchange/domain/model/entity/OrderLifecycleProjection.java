/*
 * 檔案用途：JPA read projection，保存每張訂單最新 lifecycle 狀態供查詢與 replay 重建。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "order_lifecycle_projection",
        indexes = {
                @Index(name = "idx_order_lifecycle_projection_uid_symbol", columnList = "uid,symbol,last_event_at"),
                @Index(name = "idx_order_lifecycle_projection_status", columnList = "status,last_event_at"),
                @Index(name = "idx_order_lifecycle_projection_client_order", columnList = "client_order_id")
        }
)
public class OrderLifecycleProjection {

    @Id
    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "uid", nullable = false)
    private Long uid;

    @Column(name = "symbol", nullable = false, length = 64)
    private String symbol;

    @Column(name = "client_order_id", length = 128)
    private String clientOrderId;

    @Column(name = "strategy_id", length = 128)
    private String strategyId;

    @Column(name = "market_maker_id", length = 128)
    private String marketMakerId;

    @Column(name = "latest_stage", nullable = false, length = 32)
    private String latestStage;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    @Column(name = "price", precision = 38, scale = 18)
    private BigDecimal price;

    @Column(name = "orig_qty", precision = 38, scale = 18)
    private BigDecimal origQty;

    @Column(name = "remaining_qty", precision = 38, scale = 18)
    private BigDecimal remainingQty;

    @Column(name = "executed_qty", precision = 38, scale = 18)
    private BigDecimal executedQty;

    @Column(name = "avg_price", precision = 38, scale = 18)
    private BigDecimal avgPrice;

    @Column(name = "first_event_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant firstEventAt;

    @Column(name = "last_event_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant lastEventAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    public void apply(OrderLifecycleEventRecord record) {
        if (record == null) return;
        orderId = record.getOrderId();
        schemaVersion = record.getSchemaVersion();
        uid = record.getUid();
        symbol = record.getSymbol();
        clientOrderId = record.getClientOrderId();
        strategyId = record.getStrategyId();
        marketMakerId = record.getMarketMakerId();
        latestStage = record.getStage();
        status = record.getStatus();
        reasonCode = record.getReasonCode();
        price = record.getPrice();
        origQty = record.getOrigQty();
        remainingQty = record.getRemainingQty();
        executedQty = record.getExecutedQty();
        avgPrice = record.getAvgPrice();

        Instant eventTs = record.getEventTs() == null ? Instant.now() : record.getEventTs();
        if (firstEventAt == null || eventTs.isBefore(firstEventAt)) {
            firstEventAt = eventTs;
        }
        lastEventAt = eventTs;
        updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
    }
}
