/*
 * 檔案用途：測試 ADL queue entry 到 forced execution 的 orchestration。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.usecase.ExecuteAdlUseCase;
import com.example.exchange.domain.model.dto.AdlExecutionResult;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.enums.MarginMode;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import com.example.exchange.infra.config.DefaultSymbolConfigRepository;
import com.example.exchange.infra.config.MarkPriceOracleProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdlQueueExecutionServiceTest {

    private final Symbol symbol = Symbol.builder()
            .base("BTC")
            .quote("USDT")
            .priceScale(1)
            .qtyScale(3)
            .build();

    @Test
    @DisplayName("ADL queue execution 只挑被清算倉位的對手方並完成 queue entry")
    /**
     * 流程：LONG 被清算產生 ADL 缺口 -> queue executor 只挑 profitable SHORT 候選，
     * 產生 plan 後走 ExecuteAdlUseCase 執行，最後移除已覆蓋的 queue entry。
     */
    void queueExecutionPlansAndExecutesOppositeSideCandidates() {
        Fixture fixture = fixture();
        fixture.insuranceFundService.enqueueAdl("liq-1", 7, "BTCUSDT", "LONG", new BigDecimal("100"));
        fixture.seedPosition(10, "-2", "120", "10");
        fixture.seedPosition(11, "2", "80", "10");

        AdlExecutionResult result = fixture.queueExecutionService.execute("adl-queue-1", "liq-1");

        assertThat(result.executed()).isTrue();
        assertThat(result.executedNotional()).isEqualByComparingTo("100");
        assertThat(result.steps()).singleElement()
                .satisfies(step -> {
                    assertThat(step.uid()).isEqualTo(10);
                    assertThat(step.closedQty()).isEqualByComparingTo("1.000000000000000000");
                });
        assertThat(fixture.positionRepo.find(10, symbol).orElseThrow().getQty()).isEqualByComparingTo("-1.000000000000000000");
        assertThat(fixture.positionRepo.find(11, symbol).orElseThrow().getQty()).isEqualByComparingTo("2");
        assertThat(fixture.insuranceFundService.adlQueue()).isEmpty();
        assertThat(fixture.ledgerRepo.findByUid(10)).extracting(WalletLedgerEntry::getReason)
                .contains("realized_pnl_profit", "adl_forced_loss");
    }

    @Test
    @DisplayName("已 claim 的 ADL queue entry 只能由 owner 執行")
    /**
     * 流程：ops-1 claim queue entry -> ops-2 執行被拒絕 -> ops-1 執行成功並完成 queue。
     */
    void claimedQueueEntryRequiresMatchingOperator() {
        Fixture fixture = fixture();
        fixture.insuranceFundService.enqueueAdl("liq-2", 7, "BTCUSDT", "LONG", new BigDecimal("100"));
        fixture.insuranceFundService.claimAdl("liq-2", "ops-1");
        fixture.seedPosition(10, "-2", "120", "10");

        assertThatThrownBy(() -> fixture.queueExecutionService.execute("adl-queue-2", "liq-2", "ops-2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("claimed by ops-1");

        AdlExecutionResult result = fixture.queueExecutionService.execute("adl-queue-2", "liq-2", "ops-1");

        assertThat(result.executed()).isTrue();
        assertThat(fixture.insuranceFundService.adlQueue()).isEmpty();
    }

    @Test
    @DisplayName("ADL queue partial execution 會保留剩餘缺口供下次 retry")
    /**
     * 流程：queue shortfall 大於可用候選 notional -> executor 只減掉可用候選，
     * 並把 queue amount 更新為 remainingNotional，避免下次 retry 重複承接已執行部分。
     */
    void partialQueueExecutionKeepsRemainingAmountForRetry() {
        Fixture fixture = fixture();
        fixture.insuranceFundService.enqueueAdl("liq-3", 7, "BTCUSDT", "LONG", new BigDecimal("300"));
        fixture.seedPosition(10, "-1", "120", "10");

        AdlExecutionResult result = fixture.queueExecutionService.execute("adl-queue-3", "liq-3");

        assertThat(result.executed()).isTrue();
        assertThat(result.executedNotional()).isEqualByComparingTo("100.000000000000000000");
        assertThat(result.remainingNotional()).isEqualByComparingTo("200.000000000000000000");
        assertThat(fixture.insuranceFundService.adlQueue()).singleElement()
                .satisfies(entry -> {
                    assertThat(entry.liquidationId()).isEqualTo("liq-3");
                    assertThat(entry.amount()).isEqualByComparingTo("200.000000000000000000");
                });
    }

    @Test
    @DisplayName("ADL queue 沒有合格對手方時會回 non-executed result 並保留 queue")
    /**
     * 流程：LONG 被清算但只有同方向或不獲利倉位 -> planner 產生空 plan ->
     * executor 不呼叫 forced execution、不消耗 command id，queue 保留給後續候選出現後 retry。
     */
    void queueExecutionKeepsEntryWhenNoEligibleCandidates() {
        Fixture fixture = fixture();
        fixture.insuranceFundService.enqueueAdl("liq-4", 7, "BTCUSDT", "LONG", new BigDecimal("100"));
        fixture.seedPosition(11, "2", "80", "10");

        AdlExecutionResult result = fixture.queueExecutionService.execute("adl-queue-4", "liq-4");

        assertThat(result.executed()).isFalse();
        assertThat(result.reason()).isEqualTo("ADL_NO_ELIGIBLE_CANDIDATES");
        assertThat(result.remainingNotional()).isEqualByComparingTo("100");
        assertThat(result.steps()).isEmpty();
        assertThat(fixture.insuranceFundService.adlQueue()).singleElement()
                .satisfies(entry -> {
                    assertThat(entry.liquidationId()).isEqualTo("liq-4");
                    assertThat(entry.amount()).isEqualByComparingTo("100");
                });
    }

    private Fixture fixture() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        InsuranceFundService insuranceFundService = new InsuranceFundService();
        MarkPriceOracleService markPriceOracleService = new MarkPriceOracleService(new MarkPriceOracleProperties());
        markPriceOracleService.update("BTCUSDT", new BigDecimal("100"), new BigDecimal("100"), "test");
        AdlForcedExecutionService executionService = new AdlForcedExecutionService(
                accountRepo,
                positionRepo,
                new DefaultSymbolConfigRepository(),
                walletLedgerService,
                event -> {
                }
        );
        ExecuteAdlUseCase executeAdlUseCase = new ExecuteAdlUseCase(executionService);
        AdlQueueExecutionService queueExecutionService = new AdlQueueExecutionService(
                insuranceFundService,
                positionRepo,
                markPriceOracleService,
                new AdlRankingService(),
                new AdlDeleveragingPlanner(),
                executeAdlUseCase
        );
        return new Fixture(accountRepo, ledgerRepo, positionRepo, walletLedgerService, insuranceFundService, queueExecutionService);
    }

    private record Fixture(
            MemAccountRepository accountRepo,
            MemWalletLedgerRepository ledgerRepo,
            MemPositionRepository positionRepo,
            WalletLedgerService walletLedgerService,
            InsuranceFundService insuranceFundService,
            AdlQueueExecutionService queueExecutionService
    ) {
        void seedPosition(long uid, String qty, String entryPrice, String margin) {
            walletLedgerService.deposit(uid, "USDT", new BigDecimal("1000"), "deposit-" + uid);
            walletLedgerService.increasePositionMargin(uid, "USDT", new BigDecimal(margin), "margin-" + uid);
            positionRepo.save(Position.builder()
                    .uid(uid)
                    .symbol(Symbol.builder()
                            .base("BTC")
                            .quote("USDT")
                            .priceScale(1)
                            .qtyScale(3)
                            .build())
                    .mode(MarginMode.CROSS)
                    .leverage(new BigDecimal("20"))
                    .qty(new BigDecimal(qty))
                    .entryPrice(new BigDecimal(entryPrice))
                    .margin(new BigDecimal(margin))
                    .build());
        }
    }

    private static class MemAccountRepository implements AccountRepository {
        private final Map<Long, Account> accounts = new LinkedHashMap<>();

        @Override
        public Optional<Account> findByUid(long uid) {
            return Optional.ofNullable(accounts.get(uid));
        }

        @Override
        public List<Account> findAll() {
            return new ArrayList<>(accounts.values());
        }

        @Override
        public void save(Account account) {
            accounts.put(account.uid(), account);
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
            return entries.stream().filter(entry -> entry.getUid() == uid).toList();
        }

        @Override
        public List<WalletLedgerEntry> findByRefId(String refId) {
            return entries.stream().filter(entry -> refId.equals(entry.getRefId())).toList();
        }
    }

    private static class MemPositionRepository implements PositionRepository {
        private final Map<String, Position> positions = new LinkedHashMap<>();

        @Override
        public Optional<Position> find(long uid, Symbol symbol) {
            return Optional.ofNullable(positions.get(key(uid, symbol.code())));
        }

        @Override
        public void save(Position position) {
            positions.put(key(position.getUid(), position.getSymbol().code()), position);
        }

        @Override
        public List<Position> findAllByUid(long uid) {
            return positions.values().stream().filter(position -> position.getUid() == uid).toList();
        }

        @Override
        public List<Position> findOpenPositions() {
            return positions.values().stream()
                    .filter(position -> position.getQty() != null && position.getQty().signum() != 0)
                    .toList();
        }

        private static String key(long uid, String symbol) {
            return uid + ":" + symbol;
        }
    }
}
