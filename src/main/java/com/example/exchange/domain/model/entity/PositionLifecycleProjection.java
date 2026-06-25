/*
 * 檔案用途：JPA read projection，保存使用者單一 symbol 的最新 live position SQL mirror。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.PositionLifecycleProjectionId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
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
@IdClass(PositionLifecycleProjectionId.class)
@Table(
        name = "position_lifecycle_projection",
        indexes = {
                @Index(name = "idx_position_projection_symbol_qty_updated", columnList = "symbol,qty,updated_at"),
                @Index(name = "idx_position_projection_uid_updated", columnList = "uid,updated_at"),
                @Index(name = "idx_position_projection_updated", columnList = "updated_at")
        }
)
public class PositionLifecycleProjection {

    @Id
    @Column(name = "uid", nullable = false)
    private Long uid;

    @Id
    @Column(name = "symbol", nullable = false, length = 64)
    private String symbol;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion = 1;

    @Column(name = "mode", nullable = false, length = 32)
    private String mode;

    @Column(name = "leverage", nullable = false, precision = 38, scale = 18)
    private BigDecimal leverage = BigDecimal.ONE;

    @Column(name = "qty", nullable = false, precision = 38, scale = 18)
    private BigDecimal qty = BigDecimal.ZERO;

    @Column(name = "entry_price", nullable = false, precision = 38, scale = 18)
    private BigDecimal entryPrice = BigDecimal.ZERO;

    @Column(name = "margin", nullable = false, precision = 38, scale = 18)
    private BigDecimal margin = BigDecimal.ZERO;

    @Column(name = "realized_pnl", nullable = false, precision = 38, scale = 18)
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Column(name = "fee_paid", nullable = false, precision = 38, scale = 18)
    private BigDecimal feePaid = BigDecimal.ZERO;

    @Column(name = "rebate_earned", nullable = false, precision = 38, scale = 18)
    private BigDecimal rebateEarned = BigDecimal.ZERO;

    @Column(name = "funding_paid", nullable = false, precision = 38, scale = 18)
    private BigDecimal fundingPaid = BigDecimal.ZERO;

    @Column(name = "funding_received", nullable = false, precision = 38, scale = 18)
    private BigDecimal fundingReceived = BigDecimal.ZERO;

    @Column(name = "insurance_fund_covered", nullable = false, precision = 38, scale = 18)
    private BigDecimal insuranceFundCovered = BigDecimal.ZERO;

    @Column(name = "adl_covered", nullable = false, precision = 38, scale = 18)
    private BigDecimal adlCovered = BigDecimal.ZERO;

    @Column(name = "last_trade_ref", length = 128)
    private String lastTradeRef;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}
