/*
 * 檔案用途：測試 wallet ledger hash-chain canonical hash。
 */
package com.example.exchange.domain.util;

import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletLedgerPosting;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalletLedgerHashTest {

    @Test
    @DisplayName("entryHash 對相同 ledger entry 產生穩定 hash，且 previousHash 會影響結果")
    void entryHashIsStableAndChainsPreviousHash() {
        WalletLedgerEntry entry = entry();

        // 場景：同一筆 journal entry replay 時 hash 必須穩定；前一筆 hash 不同時，chain hash 也要不同。
        String first = WalletLedgerHash.entryHash("previous-a", entry);
        String second = WalletLedgerHash.entryHash("previous-a", entry);
        String chained = WalletLedgerHash.entryHash("previous-b", entry);

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(chained);
        assertThat(first).hasSize(64);
    }

    @Test
    @DisplayName("entryHash 對 posting 金額竄改會產生不同 hash")
    void entryHashChangesWhenPostingAmountChanges() {
        WalletLedgerEntry original = entry();
        WalletLedgerEntry tampered = WalletLedgerEntry.builder()
                .id(original.getId())
                .uid(original.getUid())
                .asset(original.getAsset())
                .reason(original.getReason())
                .refId(original.getRefId())
                .amount(original.getAmount())
                .balanceAfter(original.getBalanceAfter())
                .createdAt(original.getCreatedAt())
                .postings(List.of(
                        new WalletLedgerPosting("USER_AVAILABLE", "USDT", new BigDecimal("99"), BigDecimal.ZERO),
                        new WalletLedgerPosting("EXTERNAL_CASH", "USDT", BigDecimal.ZERO, new BigDecimal("100"))
                ))
                .build();

        // 場景：posting 被改動，即使 entry id/ref/date 不變，也必須讓 hash verification 發現差異。
        assertThat(WalletLedgerHash.entryHash(WalletLedgerHash.GENESIS_HASH, original))
                .isNotEqualTo(WalletLedgerHash.entryHash(WalletLedgerHash.GENESIS_HASH, tampered));
    }

    private static WalletLedgerEntry entry() {
        return WalletLedgerEntry.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .uid(91)
                .asset("USDT")
                .reason("deposit")
                .refId("deposit-ref")
                .amount(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("100.00"))
                .createdAt(Instant.parse("2026-05-30T00:00:00Z"))
                .postings(List.of(
                        new WalletLedgerPosting("USER_AVAILABLE", "USDT", new BigDecimal("100.00"), BigDecimal.ZERO),
                        new WalletLedgerPosting("EXTERNAL_CASH", "USDT", BigDecimal.ZERO, new BigDecimal("100.00"))
                ))
                .build();
    }
}
