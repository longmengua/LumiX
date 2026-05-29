/*
 * 檔案用途：JPA entity，保存 ADL forced execution idempotency 與 audit summary。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.AdlExecutionResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "adl_execution_records",
        indexes = {
                @Index(name = "idx_adl_execution_status_time", columnList = "status,updated_at"),
                @Index(name = "idx_adl_execution_reason_time", columnList = "reason,updated_at")
        }
)
public class AdlExecutionRecordEntity {

    @Id
    @Column(name = "command_id", nullable = false, length = 128)
    private String commandId;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "reason", nullable = false, length = 128)
    private String reason;

    @Column(name = "requested_notional", nullable = false, precision = 38, scale = 18)
    private BigDecimal requestedNotional;

    @Column(name = "planned_notional", nullable = false, precision = 38, scale = 18)
    private BigDecimal plannedNotional;

    @Column(name = "executed_notional", nullable = false, precision = 38, scale = 18)
    private BigDecimal executedNotional;

    @Column(name = "remaining_notional", nullable = false, precision = 38, scale = 18)
    private BigDecimal remainingNotional;

    @Column(name = "socialized_loss_charged", nullable = false, precision = 38, scale = 18)
    private BigDecimal socializedLossCharged;

    @Column(name = "executed_at", columnDefinition = "DATETIME(6)")
    private Instant executedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    public static AdlExecutionRecordEntity started(
            String commandId,
            int schemaVersion,
            BigDecimal requestedNotional,
            BigDecimal plannedNotional,
            BigDecimal remainingNotional,
            Instant now
    ) {
        AdlExecutionRecordEntity entity = new AdlExecutionRecordEntity();
        entity.setCommandId(commandId);
        entity.setSchemaVersion(schemaVersion);
        entity.setStatus("STARTED");
        entity.setReason("STARTED");
        entity.setRequestedNotional(safe(requestedNotional));
        entity.setPlannedNotional(safe(plannedNotional));
        entity.setExecutedNotional(BigDecimal.ZERO);
        entity.setRemainingNotional(safe(remainingNotional));
        entity.setSocializedLossCharged(BigDecimal.ZERO);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public void applyResult(AdlExecutionResult result, String status) {
        setSchemaVersion(1);
        setStatus(status);
        setReason(result.reason());
        setRequestedNotional(result.requestedNotional());
        setPlannedNotional(result.plannedNotional());
        setExecutedNotional(result.executedNotional());
        setRemainingNotional(result.remainingNotional());
        setSocializedLossCharged(result.socializedLossCharged());
        setExecutedAt(result.executedAt());
        setUpdatedAt(Instant.now());
    }

    public AdlExecutionResult toResult() {
        return new AdlExecutionResult(
                commandId,
                "EXECUTED".equals(status),
                reason,
                requestedNotional,
                plannedNotional,
                executedNotional,
                remainingNotional,
                socializedLossCharged,
                List.of(),
                executedAt == null ? updatedAt : executedAt
        );
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
