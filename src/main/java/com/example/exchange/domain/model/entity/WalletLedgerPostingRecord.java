/*
 * 檔案用途：JPA journal entity，保存 durable wallet ledger posting line。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.WalletLedgerPosting;

import com.example.exchange.domain.model.dto.WalletLedgerEntry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "wallet_ledger_postings",
        indexes = {
                @Index(name = "idx_wallet_ledger_posting_entry", columnList = "entry_id"),
                @Index(name = "idx_wallet_ledger_posting_account", columnList = "asset,account_code,created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wallet_ledger_posting_line",
                        columnNames = {"entry_id", "line_no"}
                )
        }
)
public class WalletLedgerPostingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_id", nullable = false, length = 36)
    private String entryId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "account_code", nullable = false, length = 64)
    private String accountCode;

    @Column(name = "asset", nullable = false, length = 32)
    private String asset;

    @Column(name = "debit", nullable = false, precision = 38, scale = 18)
    private BigDecimal debit;

    @Column(name = "credit", nullable = false, precision = 38, scale = 18)
    private BigDecimal credit;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    public static WalletLedgerPostingRecord from(
            WalletLedgerEntry entry,
            WalletLedgerPosting posting,
            int lineNo
    ) {
        WalletLedgerPostingRecord record = new WalletLedgerPostingRecord();
        record.setEntryId(entry.getId().toString());
        record.setLineNo(lineNo);
        record.setAccountCode(posting.accountCode());
        record.setAsset(posting.asset());
        record.setDebit(posting.debit());
        record.setCredit(posting.credit());
        record.setCreatedAt(entry.getCreatedAt() == null ? Instant.now() : entry.getCreatedAt());
        return record;
    }

    public WalletLedgerPosting toPosting() {
        return new WalletLedgerPosting(accountCode, asset, debit, credit);
    }
}
