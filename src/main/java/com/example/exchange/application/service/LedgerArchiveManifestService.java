/*
 * 檔案用途：產生 ledger archive manifest，供 archive-first / verify-checksum / delete-last 流程使用。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.LedgerArchiveEligibilityReport;
import com.example.exchange.domain.model.dto.LedgerArchiveManifest;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletLedgerPosting;
import com.example.exchange.domain.repository.WalletLedgerJournal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerArchiveManifestService {

    private static final int SCHEMA_VERSION = 1;

    private final WalletLedgerJournal ledgerJournal;
    private final LedgerArchiveEligibilityService eligibilityService;

    public LedgerArchiveManifest generate(LocalDate reportDate) {
        LocalDate date = reportDate == null ? LocalDate.now(ZoneOffset.UTC).minusDays(1) : reportDate;
        Instant from = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<WalletLedgerEntry> entries = ledgerJournal.findByCreatedAtBetween(from, to);
        LedgerArchiveEligibilityReport eligibility = eligibilityService.evaluate(date);
        long postingCount = entries.stream()
                .mapToLong(entry -> entry.getPostings() == null ? 0 : entry.getPostings().size())
                .sum();
        String checksum = checksum(entries);
        return new LedgerArchiveManifest(
                "ledger-" + date + "-" + checksum.substring(0, 16),
                "wallet_ledger",
                SCHEMA_VERSION,
                date,
                from,
                to,
                entries.size(),
                postingCount,
                checksum,
                eligibility.deleteEligible(),
                null,
                Instant.now()
        );
    }

    private static String checksum(List<WalletLedgerEntry> entries) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (WalletLedgerEntry entry : entries) {
                digest.update(canonical(entry).getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("hash ledger archive manifest failed", e);
        }
    }

    private static String canonical(WalletLedgerEntry entry) {
        StringBuilder builder = new StringBuilder();
        builder.append(entry.getId()).append('|')
                .append(entry.getUid()).append('|')
                .append(entry.getAsset()).append('|')
                .append(entry.getReason()).append('|')
                .append(entry.getRefId()).append('|')
                .append(entry.getAmount()).append('|')
                .append(entry.getBalanceAfter()).append('|')
                .append(entry.getCreatedAt());
        for (WalletLedgerPosting posting : entry.getPostings() == null ? List.<WalletLedgerPosting>of() : entry.getPostings()) {
            builder.append('|')
                    .append(posting.accountCode()).append(':')
                    .append(posting.asset()).append(':')
                    .append(posting.debit()).append(':')
                    .append(posting.credit());
        }
        return builder.toString();
    }
}
