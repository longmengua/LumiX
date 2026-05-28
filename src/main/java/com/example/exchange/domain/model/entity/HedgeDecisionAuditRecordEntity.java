/*
 * 檔案用途：JPA entity，保存做市商 hedge decision audit trail。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.HedgeDecisionAuditRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "hedge_decision_audits",
        indexes = {
                @Index(name = "idx_hedge_decision_mm_time", columnList = "market_maker_id,decided_at"),
                @Index(name = "idx_hedge_decision_symbol_time", columnList = "symbol,decided_at"),
                @Index(name = "idx_hedge_decision_ref", columnList = "ref_id"),
                @Index(name = "idx_hedge_decision_accepted", columnList = "accepted,decided_at")
        }
)
public class HedgeDecisionAuditRecordEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "market_maker_id", nullable = false, length = 128)
    private String marketMakerId;

    @Column(name = "symbol", nullable = false, length = 32)
    private String symbol;

    @Column(name = "accepted", nullable = false)
    private Boolean accepted;

    @Column(name = "reason", length = 128)
    private String reason;

    @Column(name = "order_notional", nullable = false, precision = 38, scale = 18)
    private BigDecimal orderNotional;

    @Column(name = "venue_order_id", length = 128)
    private String venueOrderId;

    @Column(name = "ref_id", length = 128)
    private String refId;

    @Column(name = "decided_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    public static HedgeDecisionAuditRecordEntity from(HedgeDecisionAuditRecord record, int schemaVersion) {
        HedgeDecisionAuditRecordEntity entity = new HedgeDecisionAuditRecordEntity();
        entity.setId(record.id().toString());
        entity.setSchemaVersion(schemaVersion);
        entity.setMarketMakerId(record.marketMakerId());
        entity.setSymbol(record.symbol());
        entity.setAccepted(record.accepted());
        entity.setReason(record.reason());
        entity.setOrderNotional(record.orderNotional());
        entity.setVenueOrderId(record.venueOrderId());
        entity.setRefId(record.refId());
        entity.setDecidedAt(record.decidedAt());
        entity.setCreatedAt(record.createdAt());
        return entity;
    }

    public HedgeDecisionAuditRecord toRecord() {
        return new HedgeDecisionAuditRecord(
                UUID.fromString(id),
                marketMakerId,
                symbol,
                accepted,
                reason,
                orderNotional,
                venueOrderId,
                refId,
                decidedAt,
                createdAt
        );
    }
}
