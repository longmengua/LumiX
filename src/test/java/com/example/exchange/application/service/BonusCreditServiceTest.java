/*
 * 檔案用途：測試 BonusCreditService 的體驗金批次、FIFO consume 與到期掃描。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.BonusCreditGrant;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.BonusCreditGrantStore;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BonusCreditServiceTest {

    @Test
    @DisplayName("consume 依 expiresAt FIFO 扣體驗金批次並同步 ledger")
    void consumeUsesExpiryFifoAndUpdatesGrantRemaining() {
        Fixtures fixtures = fixtures("2026-05-27T00:00:00Z");

        // 流程：兩筆不同到期日的體驗金，消耗 12 時先扣較早到期的 10，再扣第二筆 2。
        BonusCreditGrant first = fixtures.service.grant(
                61,
                "USDT",
                new BigDecimal("10.00"),
                "campaign-a",
                Instant.parse("2026-05-28T00:00:00Z"),
                "grant-a"
        );
        BonusCreditGrant second = fixtures.service.grant(
                61,
                "USDT",
                new BigDecimal("15.00"),
                "campaign-b",
                Instant.parse("2026-05-29T00:00:00Z"),
                "grant-b"
        );

        BigDecimal consumed = fixtures.service.consume(61, "USDT", new BigDecimal("12.00"), "trade-fee", "USER_FEE_EXPENSE");

        assertThat(consumed).isEqualByComparingTo("12.00");
        assertThat(fixtures.grantStore.records.get(first.id()).status()).isEqualTo(BonusCreditGrant.CONSUMED);
        assertThat(fixtures.grantStore.records.get(first.id()).remainingAmount()).isEqualByComparingTo("0.00");
        assertThat(fixtures.grantStore.records.get(second.id()).status()).isEqualTo(BonusCreditGrant.ACTIVE);
        assertThat(fixtures.grantStore.records.get(second.id()).remainingAmount()).isEqualByComparingTo("13.00");
        assertThat(fixtures.walletLedgerService.bonusCreditBalance(61, "USDT")).isEqualByComparingTo("13.00");
    }

    @Test
    @DisplayName("expireDue 只過期已到期且仍有 remaining 的 active grant")
    void expireDueExpiresOnlyRemainingActiveGrants() {
        Fixtures fixtures = fixtures("2026-05-30T00:00:00Z");
        fixtures.service.grant(
                62,
                "USDT",
                new BigDecimal("8.00"),
                "campaign-expired",
                Instant.parse("2026-05-29T00:00:00Z"),
                "grant-expired"
        );
        BonusCreditGrant future = fixtures.service.grant(
                62,
                "USDT",
                new BigDecimal("5.00"),
                "campaign-future",
                Instant.parse("2026-06-01T00:00:00Z"),
                "grant-future"
        );

        // 流程：scanner 在 2026-05-30 執行，只會把 2026-05-29 到期的批次寫 expire ledger。
        int expired = fixtures.service.expireDue(100);

        assertThat(expired).isEqualTo(1);
        assertThat(fixtures.walletLedgerService.bonusCreditBalance(62, "USDT")).isEqualByComparingTo("5.00");
        assertThat(fixtures.grantStore.records.get(future.id()).status()).isEqualTo(BonusCreditGrant.ACTIVE);
        assertThat(fixtures.ledgerRepository.findByUid(62))
                .extracting(WalletLedgerEntry::getReason)
                .contains("bonus_credit_expire");
    }

    private static Fixtures fixtures(String now) {
        MemAccountRepository accountRepository = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepository = new MemWalletLedgerRepository();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepository, ledgerRepository);
        MemBonusCreditGrantStore grantStore = new MemBonusCreditGrantStore();
        BonusCreditService service = new BonusCreditService(
                grantStore,
                walletLedgerService,
                Clock.fixed(Instant.parse(now), ZoneOffset.UTC)
        );
        return new Fixtures(grantStore, ledgerRepository, walletLedgerService, service);
    }

    private record Fixtures(
            MemBonusCreditGrantStore grantStore,
            MemWalletLedgerRepository ledgerRepository,
            WalletLedgerService walletLedgerService,
            BonusCreditService service
    ) {
    }

    private static class MemBonusCreditGrantStore implements BonusCreditGrantStore {
        private final Map<java.util.UUID, BonusCreditGrant> records = new LinkedHashMap<>();

        @Override
        public void save(BonusCreditGrant grant) {
            records.put(grant.id(), grant);
        }

        @Override
        public List<BonusCreditGrant> findActiveByUidAndAsset(long uid, String asset) {
            return records.values().stream()
                    .filter(grant -> grant.uid() == uid)
                    .filter(grant -> asset.equals(grant.asset()))
                    .filter(grant -> BonusCreditGrant.ACTIVE.equals(grant.status()))
                    .sorted(Comparator
                            .comparing(BonusCreditServiceTest::sortableExpiry)
                            .thenComparing(BonusCreditGrant::grantedAt)
                            .thenComparing(BonusCreditGrant::id))
                    .toList();
        }

        @Override
        public List<BonusCreditGrant> findActiveExpiringAtOrBefore(Instant now, int limit) {
            return records.values().stream()
                    .filter(grant -> BonusCreditGrant.ACTIVE.equals(grant.status()))
                    .filter(grant -> grant.expiresAt() != null && !grant.expiresAt().isAfter(now))
                    .sorted(Comparator.comparing(BonusCreditGrant::expiresAt).thenComparing(BonusCreditGrant::id))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<BonusCreditGrant> findByUid(long uid) {
            return records.values().stream()
                    .filter(grant -> grant.uid() == uid)
                    .toList();
        }
    }

    private static Instant sortableExpiry(BonusCreditGrant grant) {
        return grant.expiresAt() == null ? Instant.MAX : grant.expiresAt();
    }

    private static class MemAccountRepository implements AccountRepository {
        @Override
        public Optional<Account> findByUid(long uid) {
            return Optional.empty();
        }

        @Override
        public void save(Account account) {
        }
    }

    private static class MemWalletLedgerRepository implements WalletLedgerRepository {
        private final List<WalletLedgerEntry> entries = new ArrayList<>();

        @Override
        public void append(WalletLedgerEntry entry) {
            assertThat(entry.isBalanced()).isTrue();
            entries.add(entry);
        }

        @Override
        public List<WalletLedgerEntry> findByUid(long uid) {
            return entries.stream()
                    .filter(entry -> entry.getUid() == uid)
                    .toList();
        }

        @Override
        public List<WalletLedgerEntry> findByRefId(String refId) {
            return entries.stream()
                    .filter(entry -> refId.equals(entry.getRefId()))
                    .toList();
        }
    }
}
