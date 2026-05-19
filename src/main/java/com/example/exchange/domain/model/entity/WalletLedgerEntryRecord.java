/*
 * 檔案用途：JPA journal entity，保存 durable wallet ledger entry。
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

@Getter
@Setter
@Entity
@Table(
        name = "wallet_ledger_entries",
        indexes = {
                @Index(name = "idx_wallet_ledger_uid_asset_created", columnList = "uid,asset,created_at"),
                @Index(name = "idx_wallet_ledger_ref", columnList = "ref_id"),
                @Index(name = "idx_wallet_ledger_reason_created", columnList = "reason,created_at")
        }
)
public class WalletLedgerEntryRecord {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "uid", nullable = false)
    private Long uid;

    @Column(name = "asset", nullable = false, length = 32)
    private String asset;

    @Column(name = "reason", nullable = false, length = 64)
    private String reason;

    @Column(name = "ref_id", length = 128)
    private String refId;

    @Column(name = "amount", nullable = false, precision = 38, scale = 18)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 38, scale = 18)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    public static WalletLedgerEntryRecord from(WalletLedgerEntry entry, int schemaVersion) {
        WalletLedgerEntryRecord record = new WalletLedgerEntryRecord();
        record.setId(entry.getId().toString());
        record.setSchemaVersion(schemaVersion);
        record.setUid(entry.getUid());
        record.setAsset(entry.getAsset());
        record.setReason(entry.getReason());
        record.setRefId(blankToNull(entry.getRefId()));
        record.setAmount(entry.getAmount());
        record.setBalanceAfter(entry.getBalanceAfter());
        record.setCreatedAt(entry.getCreatedAt() == null ? Instant.now() : entry.getCreatedAt());
        return record;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
