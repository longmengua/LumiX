/*
 * 檔案用途：JPA entity，保存每日 account risk snapshot read model。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.AccountRiskSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "account_risk_snapshots",
        indexes = {
                @Index(name = "idx_account_risk_snapshots_uid_day", columnList = "uid,snapshot_date"),
                @Index(name = "idx_account_risk_snapshots_uid_time", columnList = "uid,calculated_at")
        }
)
public class AccountRiskSnapshotRecord {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "uid", nullable = false)
    private Long uid;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "cross_balance", nullable = false, precision = 38, scale = 18)
    private BigDecimal crossBalance;

    @Column(name = "available_balance", nullable = false, precision = 38, scale = 18)
    private BigDecimal availableBalance;

    @Column(name = "order_hold", nullable = false, precision = 38, scale = 18)
    private BigDecimal orderHold;

    @Column(name = "position_margin", nullable = false, precision = 38, scale = 18)
    private BigDecimal positionMargin;

    @Column(name = "frozen_funds", nullable = false, precision = 38, scale = 18)
    private BigDecimal frozenFunds;

    @Column(name = "unrealized_pnl", nullable = false, precision = 38, scale = 18)
    private BigDecimal unrealizedPnl;

    @Column(name = "total_equity", nullable = false, precision = 38, scale = 18)
    private BigDecimal totalEquity;

    @Column(name = "maintenance_margin", nullable = false, precision = 38, scale = 18)
    private BigDecimal maintenanceMargin;

    @Column(name = "risk_ratio", nullable = false, precision = 38, scale = 18)
    private BigDecimal riskRatio;

    @Column(name = "open_position_count", nullable = false)
    private Integer openPositionCount;

    @Column(name = "calculated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant calculatedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    public static AccountRiskSnapshotRecord from(AccountRiskSnapshot snapshot, int schemaVersion) {
        AccountRiskSnapshotRecord record = new AccountRiskSnapshotRecord();
        record.setId(UUID.randomUUID().toString());
        record.setSchemaVersion(schemaVersion);
        record.setUid(snapshot.uid());
        record.setSnapshotDate(snapshot.calculatedAt().atZone(ZoneOffset.UTC).toLocalDate());
        record.setCrossBalance(snapshot.crossBalance());
        record.setAvailableBalance(snapshot.availableBalance());
        record.setOrderHold(snapshot.orderHold());
        record.setPositionMargin(snapshot.positionMargin());
        record.setFrozenFunds(snapshot.frozenFunds());
        record.setUnrealizedPnl(snapshot.unrealizedPnl());
        record.setTotalEquity(snapshot.totalEquity());
        record.setMaintenanceMargin(snapshot.maintenanceMargin());
        record.setRiskRatio(snapshot.riskRatio());
        record.setOpenPositionCount(snapshot.openPositionCount());
        record.setCalculatedAt(snapshot.calculatedAt());
        record.setCreatedAt(Instant.now());
        return record;
    }

    public AccountRiskSnapshot toSnapshot() {
        return new AccountRiskSnapshot(
                uid,
                crossBalance,
                availableBalance,
                orderHold,
                positionMargin,
                frozenFunds,
                unrealizedPnl,
                totalEquity,
                maintenanceMargin,
                riskRatio,
                openPositionCount,
                calculatedAt
        );
    }
}
