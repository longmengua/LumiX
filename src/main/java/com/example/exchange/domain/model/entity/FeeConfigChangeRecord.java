/*
 * File purpose: JPA entity for immutable admin fee-configuration change history.
 */
package com.example.exchange.domain.model.entity;

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
        name = "fee_config_change_log",
        indexes = {
                @Index(name = "idx_fee_config_symbol_effective", columnList = "symbol,effective_at"),
                @Index(name = "idx_fee_config_operator_changed", columnList = "operator_id,changed_at")
        }
)
public class FeeConfigChangeRecord {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "symbol", nullable = false, length = 32)
    private String symbol;

    @Column(name = "old_maker_fee_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal oldMakerFeeRate;

    @Column(name = "old_taker_fee_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal oldTakerFeeRate;

    @Column(name = "new_maker_fee_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal newMakerFeeRate;

    @Column(name = "new_taker_fee_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal newTakerFeeRate;

    @Column(name = "operator_id", nullable = false, length = 128)
    private String operatorId;

    @Column(name = "reason", nullable = false, length = 512)
    private String reason;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @Column(name = "effective_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant effectiveAt;

    @Column(name = "changed_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant changedAt;

    public static FeeConfigChangeRecord create(
            String symbol,
            BigDecimal oldMakerFeeRate,
            BigDecimal oldTakerFeeRate,
            BigDecimal newMakerFeeRate,
            BigDecimal newTakerFeeRate,
            String operatorId,
            String reason,
            String requestId,
            Instant effectiveAt,
            Instant changedAt
    ) {
        FeeConfigChangeRecord record = new FeeConfigChangeRecord();
        // UUID keeps the audit row append-only and independent from market configuration storage.
        record.setId(UUID.randomUUID().toString());
        record.setSymbol(symbol);
        record.setOldMakerFeeRate(oldMakerFeeRate);
        record.setOldTakerFeeRate(oldTakerFeeRate);
        record.setNewMakerFeeRate(newMakerFeeRate);
        record.setNewTakerFeeRate(newTakerFeeRate);
        record.setOperatorId(operatorId);
        record.setReason(reason);
        record.setRequestId(requestId);
        record.setEffectiveAt(effectiveAt);
        record.setChangedAt(changedAt);
        return record;
    }
}
