/*
 * 檔案用途：wallet ledger journal hash-chain canonicalization 與 SHA-256 計算。
 */
package com.example.exchange.domain.util;

import com.example.exchange.domain.model.dto.WalletLedgerEntry;
import com.example.exchange.domain.model.dto.WalletLedgerPosting;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

public final class WalletLedgerHash {

    public static final String GENESIS_HASH = "GENESIS";

    private WalletLedgerHash() {
    }

    public static String entryHash(String previousHash, WalletLedgerEntry entry) {
        String canonical = canonical(previousHash, entry);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("hash wallet ledger entry failed", e);
        }
    }

    public static String canonical(String previousHash, WalletLedgerEntry entry) {
        StringBuilder builder = new StringBuilder();
        builder.append(nullToEmpty(previousHash)).append('|')
                .append(entry.getId()).append('|')
                .append(entry.getUid()).append('|')
                .append(nullToEmpty(entry.getAsset())).append('|')
                .append(nullToEmpty(entry.getReason())).append('|')
                .append(nullToEmpty(entry.getRefId())).append('|')
                .append(number(entry.getAmount())).append('|')
                .append(number(entry.getBalanceAfter())).append('|')
                .append(entry.getCreatedAt());
        List<WalletLedgerPosting> postings = entry.getPostings() == null ? List.of() : entry.getPostings();
        postings.stream()
                .sorted(Comparator
                        .comparing(WalletLedgerPosting::accountCode)
                        .thenComparing(WalletLedgerPosting::asset)
                        .thenComparing(posting -> number(posting.debit()))
                        .thenComparing(posting -> number(posting.credit())))
                .forEach(posting -> builder.append('|')
                        .append(nullToEmpty(posting.accountCode())).append(':')
                        .append(nullToEmpty(posting.asset())).append(':')
                        .append(number(posting.debit())).append(':')
                        .append(number(posting.credit())));
        return builder.toString();
    }

    private static String number(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
