/*
 * 檔案用途：JPA event log，保存 order lifecycle event 的 durable append-only 紀錄。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.event.OrderLifecycleEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "order_lifecycle_events",
        indexes = {
                @Index(name = "idx_order_lifecycle_order", columnList = "order_id,event_ts"),
                @Index(name = "idx_order_lifecycle_uid_symbol", columnList = "uid,symbol,event_ts"),
                @Index(name = "idx_order_lifecycle_stage_ts", columnList = "stage,event_ts"),
                @Index(name = "idx_order_lifecycle_client_order", columnList = "client_order_id")
        }
)
public class OrderLifecycleEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

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

    @Column(name = "stage", nullable = false, length = 32)
    private String stage;

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

    @Column(name = "event_ts", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant eventTs;

    @Column(name = "recorded_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant recordedAt;

    public static OrderLifecycleEventRecord from(OrderLifecycleEvent event, int schemaVersion) {
        OrderLifecycleEventRecord record = new OrderLifecycleEventRecord();
        record.setSchemaVersion(schemaVersion);
        record.setOrderId(event.orderId().toString());
        record.setUid(event.uid());
        record.setSymbol(event.symbol() == null ? "UNKNOWN" : event.symbol().code());
        record.setClientOrderId(blankToNull(event.clientOrderId()));
        record.setStrategyId(blankToNull(event.strategyId()));
        record.setMarketMakerId(blankToNull(event.marketMakerId()));
        record.setStage(event.stage().name());
        record.setStatus(event.status().name());
        record.setReasonCode(blankToNull(event.reasonCode()));
        record.setPrice(event.price());
        record.setOrigQty(event.origQty());
        record.setRemainingQty(event.remainingQty());
        record.setExecutedQty(event.executedQty());
        record.setAvgPrice(event.avgPrice());
        record.setEventTs(event.ts() == null ? Instant.now() : event.ts());
        record.setRecordedAt(Instant.now());
        return record;
    }

    @PrePersist
    public void prePersist() {
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
        if (eventTs == null) {
            eventTs = recordedAt;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
