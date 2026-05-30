/*
 * 檔案用途：JPA entity，保存 trial balance 每日快照。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "trial_balance_snapshots",
        indexes = {
                @Index(name = "idx_trial_balance_snapshot_date", columnList = "report_date,uid")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_trial_balance_snapshot_scope",
                        columnNames = {"report_date", "uid", "asset"}
                )
        }
)
public class TrialBalanceSnapshotEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "uid", nullable = false)
    private Long uid;

    @Column(name = "asset", nullable = false, length = 32)
    private String asset;

    @Column(name = "total_debit", nullable = false, precision = 38, scale = 18)
    private BigDecimal totalDebit;

    @Column(name = "total_credit", nullable = false, precision = 38, scale = 18)
    private BigDecimal totalCredit;

    @Column(name = "balanced", nullable = false)
    private Boolean balanced;

    @Column(name = "generated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant generatedAt;

    @Column(name = "lines_payload", nullable = false, columnDefinition = "JSON")
    private String linesPayload;
}
