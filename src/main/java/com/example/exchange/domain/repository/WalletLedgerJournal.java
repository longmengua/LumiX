/*
 * 檔案用途：Repository 介面，定義 durable wallet ledger journal 的存取契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.WalletLedgerEntry;
import com.example.exchange.domain.model.dto.LedgerTamperEvidenceReport;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WalletLedgerJournal {

    int SCHEMA_VERSION = 1;

    void append(WalletLedgerEntry entry);

    List<WalletLedgerEntry> findByUid(long uid);

    List<WalletLedgerEntry> findByUidAndAsset(long uid, String asset);

    Optional<WalletLedgerEntry> findLatestByUidAndAsset(long uid, String asset);

    List<WalletLedgerEntry> findByRefId(String refId);

    default List<WalletLedgerEntry> findByCreatedAtBetween(Instant fromInclusive, Instant toExclusive) {
        return List.of();
    }

    default LedgerTamperEvidenceReport verifyTamperEvidence() {
        return new LedgerTamperEvidenceReport(0, 0, Instant.now(), List.of());
    }

    static void validateBalancedEntry(WalletLedgerEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("wallet ledger entry cannot be null");
        }
        if (entry.getPostings() == null || entry.getPostings().isEmpty()) {
            throw new IllegalArgumentException("wallet ledger entry must have postings");
        }
        entry.getPostings().forEach(posting -> {
            if (posting.debit().signum() < 0 || posting.credit().signum() < 0) {
                throw new IllegalArgumentException("wallet ledger posting debit/credit must be non-negative");
            }
            if (posting.debit().signum() == posting.credit().signum()) {
                throw new IllegalArgumentException("wallet ledger posting must have exactly one positive side");
            }
        });
        if (!entry.isBalanced()) {
            throw new IllegalArgumentException("wallet ledger entry must be balanced");
        }
    }
}
