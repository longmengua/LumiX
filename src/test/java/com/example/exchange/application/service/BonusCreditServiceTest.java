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
import com.example.exchange.infra.config.BonusCreditProperties;
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

    @Test
    @DisplayName("clawback 依 active grant FIFO 追回體驗金並產生報表")
    void clawbackUsesActiveGrantFifoAndReportSummarizesState() {
        Fixtures fixtures = fixtures("2026-05-30T00:00:00Z");
        BonusCreditGrant first = fixtures.service.grant(
                63,
                "USDT",
                new BigDecimal("10.00"),
                "campaign-a",
                Instant.parse("2026-06-01T00:00:00Z"),
                "grant-a"
        );
        BonusCreditGrant second = fixtures.service.grant(
                63,
                "USDT",
                new BigDecimal("15.00"),
                "campaign-b",
                Instant.parse("2026-06-02T00:00:00Z"),
                "grant-b"
        );

        // 流程：追回 12 時先完整追回第一批 10，再部分追回第二批 2，ledger 與 report 都要可追。
        BigDecimal clawedBack = fixtures.service.clawback(63, "USDT", new BigDecimal("12.00"), "ops-clawback");

        assertThat(clawedBack).isEqualByComparingTo("12.00");
        assertThat(fixtures.grantStore.records.get(first.id()).status()).isEqualTo(BonusCreditGrant.CLAWED_BACK);
        assertThat(fixtures.grantStore.records.get(second.id()).status()).isEqualTo(BonusCreditGrant.ACTIVE);
        assertThat(fixtures.grantStore.records.get(second.id()).remainingAmount()).isEqualByComparingTo("13.00");
        assertThat(fixtures.walletLedgerService.bonusCreditBalance(63, "USDT")).isEqualByComparingTo("13.00");

        var report = fixtures.service.report(63, "usdt");
        assertThat(report.asset()).isEqualTo("USDT");
        assertThat(report.totalGranted()).isEqualByComparingTo("25.00");
        assertThat(report.totalRemaining()).isEqualByComparingTo("13.00");
        assertThat(report.activeGrantCount()).isEqualTo(1);
        assertThat(report.clawedBackGrantCount()).isEqualTo(1);
        assertThat(report.clawedBackOriginalAmount()).isEqualByComparingTo("10.00");
        assertThat(report.nextExpiryAt()).isEqualTo(Instant.parse("2026-06-02T00:00:00Z"));
        assertThat(fixtures.ledgerRepository.findByUid(63))
                .extracting(WalletLedgerEntry::getReason)
                .contains("bonus_credit_clawback");
    }

    @Test
    @DisplayName("consume 啟用 eligibility 後會依 symbol/orderType/expenseAccount gate 控制體驗金使用")
    void consumeAppliesEligibilityGateWhenEnabled() {
        BonusCreditProperties properties = new BonusCreditProperties();
        properties.getEligibility().setEnabled(true);
        properties.getEligibility().setAllowedSymbols(List.of("BTCUSDT"));
        properties.getEligibility().setAllowedOrderTypes(List.of("LIMIT"));
        properties.getEligibility().setAllowedExpenseAccounts(List.of("USER_FEE_EXPENSE"));
        Fixtures fixtures = fixtures("2026-05-30T00:00:00Z", properties);
        fixtures.service.grant(
                64,
                "USDT",
                new BigDecimal("20.00"),
                "campaign-a",
                Instant.parse("2026-06-01T00:00:00Z"),
                "grant-a"
        );

        // 場景：同一筆體驗金只允許 BTCUSDT/LIMIT/fee 消耗，不符合產品規則時不得寫 ledger。
        BigDecimal rejected = fixtures.service.consume(
                64,
                "USDT",
                new BigDecimal("5.00"),
                "bonus-use",
                "USER_FEE_EXPENSE",
                "ETHUSDT",
                "LIMIT"
        );
        BigDecimal accepted = fixtures.service.consume(
                64,
                "USDT",
                new BigDecimal("5.00"),
                "bonus-use",
                "USER_FEE_EXPENSE",
                "BTCUSDT",
                "LIMIT"
        );

        assertThat(rejected).isZero();
        assertThat(accepted).isEqualByComparingTo("5.00");
        assertThat(fixtures.walletLedgerService.bonusCreditBalance(64, "USDT")).isEqualByComparingTo("15.00");
        assertThat(fixtures.ledgerRepository.findByUid(64))
                .extracting(WalletLedgerEntry::getReason)
                .containsOnly("bonus_credit_grant", "bonus_credit_consume");
    }

    @Test
    @DisplayName("eligibility blockedSymbols 優先於 allowedSymbols")
    void eligibilityBlockedSymbolsOverrideAllowedSymbols() {
        BonusCreditProperties properties = new BonusCreditProperties();
        properties.getEligibility().setEnabled(true);
        properties.getEligibility().setAllowedSymbols(List.of("BTCUSDT"));
        properties.getEligibility().setBlockedSymbols(List.of("BTCUSDT"));
        Fixtures fixtures = fixtures("2026-05-30T00:00:00Z", properties);
        fixtures.service.grant(
                65,
                "USDT",
                new BigDecimal("20.00"),
                "campaign-a",
                Instant.parse("2026-06-01T00:00:00Z"),
                "grant-a"
        );

        // 場景：營運可緊急封鎖指定 symbol，即使該 symbol 原本在 allow-list 內。
        BigDecimal consumed = fixtures.service.consume(
                65,
                "USDT",
                new BigDecimal("5.00"),
                "bonus-use",
                "USER_FEE_EXPENSE",
                "BTCUSDT",
                "LIMIT"
        );

        assertThat(consumed).isZero();
        assertThat(fixtures.walletLedgerService.bonusCreditBalance(65, "USDT")).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("campaignReport 彙總同活動跨使用者 grant 狀態")
    void campaignReportAggregatesAcrossUsers() {
        Fixtures fixtures = fixtures("2026-05-30T00:00:00Z");
        fixtures.service.grant(
                66,
                "USDT",
                new BigDecimal("10.00"),
                "campaign-a",
                Instant.parse("2026-06-01T00:00:00Z"),
                "grant-a"
        );
        fixtures.service.grant(
                67,
                "USDT",
                new BigDecimal("15.00"),
                "campaign-a",
                Instant.parse("2026-06-02T00:00:00Z"),
                "grant-b"
        );
        fixtures.service.grant(
                68,
                "USDT",
                new BigDecimal("99.00"),
                "campaign-b",
                Instant.parse("2026-06-03T00:00:00Z"),
                "grant-c"
        );
        fixtures.service.consume(66, "USDT", new BigDecimal("4.00"), "trade-fee", "USER_FEE_EXPENSE");

        // 場景：營運要看單一 campaign 的跨用戶 grant、remaining、狀態與最近到期日。
        var report = fixtures.service.campaignReport("campaign-a", "usdt");

        assertThat(report.campaignId()).isEqualTo("campaign-a");
        assertThat(report.asset()).isEqualTo("USDT");
        assertThat(report.userCount()).isEqualTo(2);
        assertThat(report.totalGranted()).isEqualByComparingTo("25.00");
        assertThat(report.totalRemaining()).isEqualByComparingTo("21.00");
        assertThat(report.activeGrantCount()).isEqualTo(2);
        assertThat(report.nextExpiryAt()).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(report.grants()).extracting(BonusCreditGrant::uid).containsExactly(66L, 67L);
    }

    @Test
    @DisplayName("clawbackCampaign 只追回指定 campaign active grants 並遵守單次上限")
    void clawbackCampaignCapsRunAndSkipsOtherCampaigns() {
        Fixtures fixtures = fixtures("2026-05-30T00:00:00Z");
        BonusCreditGrant first = fixtures.service.grant(
                69,
                "USDT",
                new BigDecimal("10.00"),
                "campaign-a",
                Instant.parse("2026-06-01T00:00:00Z"),
                "grant-a"
        );
        BonusCreditGrant second = fixtures.service.grant(
                70,
                "USDT",
                new BigDecimal("15.00"),
                "campaign-a",
                Instant.parse("2026-06-02T00:00:00Z"),
                "grant-b"
        );
        BonusCreditGrant otherCampaign = fixtures.service.grant(
                71,
                "USDT",
                new BigDecimal("99.00"),
                "campaign-b",
                Instant.parse("2026-06-01T00:00:00Z"),
                "grant-c"
        );

        // 場景：自動追回 campaign-a 單次最多 12，先追回較早到期 grant，再部分追回下一筆。
        BigDecimal clawedBack = fixtures.service.clawbackCampaign(
                "campaign-a",
                "USDT",
                new BigDecimal("12.00"),
                "auto-clawback-run"
        );

        assertThat(clawedBack).isEqualByComparingTo("12.00");
        assertThat(fixtures.grantStore.records.get(first.id()).status()).isEqualTo(BonusCreditGrant.CLAWED_BACK);
        assertThat(fixtures.grantStore.records.get(second.id()).status()).isEqualTo(BonusCreditGrant.ACTIVE);
        assertThat(fixtures.grantStore.records.get(second.id()).remainingAmount()).isEqualByComparingTo("13.00");
        assertThat(fixtures.grantStore.records.get(otherCampaign.id()).status()).isEqualTo(BonusCreditGrant.ACTIVE);
        assertThat(fixtures.grantStore.records.get(otherCampaign.id()).remainingAmount()).isEqualByComparingTo("99.00");
        assertThat(fixtures.walletLedgerService.bonusCreditBalance(69, "USDT")).isZero();
        assertThat(fixtures.walletLedgerService.bonusCreditBalance(70, "USDT")).isEqualByComparingTo("13.00");
        assertThat(fixtures.walletLedgerService.bonusCreditBalance(71, "USDT")).isEqualByComparingTo("99.00");
    }

    private static Fixtures fixtures(String now) {
        return fixtures(now, new BonusCreditProperties());
    }

    private static Fixtures fixtures(String now, BonusCreditProperties properties) {
        MemAccountRepository accountRepository = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepository = new MemWalletLedgerRepository();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepository, ledgerRepository);
        MemBonusCreditGrantStore grantStore = new MemBonusCreditGrantStore();
        BonusCreditService service = new BonusCreditService(
                grantStore,
                walletLedgerService,
                properties,
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

        @Override
        public List<BonusCreditGrant> findByCampaignId(String campaignId) {
            return records.values().stream()
                    .filter(grant -> campaignId.equals(grant.campaignId()))
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
