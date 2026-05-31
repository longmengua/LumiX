/*
 * 檔案用途：測試 ADL forced execution 對持倉、帳務、audit 與 idempotency 的影響。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.AdlForcedDeleveragingRecorded;
import com.example.exchange.domain.model.dto.AdlDeleveragingPlan;
import com.example.exchange.domain.model.dto.AdlDeleveragingStep;
import com.example.exchange.domain.model.dto.AdlExecutionResult;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.enums.MarginMode;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.AdlExecutionStore;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import com.example.exchange.infra.config.DefaultSymbolConfigRepository;
import com.example.exchange.infra.config.RiskControlsProperties;
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

class AdlForcedExecutionServiceTest {

    private final Symbol symbol = Symbol.builder()
            .base("BTC")
            .quote("USDT")
            .priceScale(1)
            .qtyScale(3)
            .build();

    @Test
    @DisplayName("ADL execution 會強制減倉並以 realized profit 承接社會化缺口")
    /**
     * 流程：高獲利多單被 ADL 選中 -> executor 依 plan 平掉一半倉位 ->
     * realized_pnl_profit 先入帳，再用 adl_forced_loss 扣回獲利用於承接清算缺口。
     */
    void adlExecutionReducesPositionAndWritesBalancedLedger() {
        Fixture fixture = fixture();
        fixture.seedLongPosition(10, "2", "80", "10");

        AdlExecutionResult result = fixture.service.execute("adl-1", plan("100", "1"));

        assertThat(result.executed()).isTrue();
        assertThat(result.executedNotional()).isEqualByComparingTo("100");
        assertThat(result.socializedLossCharged()).isEqualByComparingTo("20");
        assertThat(result.steps()).singleElement()
                .satisfies(step -> {
                    assertThat(step.closedQty()).isEqualByComparingTo("-1");
                    assertThat(step.realizedPnl()).isEqualByComparingTo("20");
                    assertThat(step.marginReleased()).isEqualByComparingTo("5.000000000000000000");
                    assertThat(step.socializedLossCharged()).isEqualByComparingTo("20");
                });

        Position position = fixture.positionRepo.find(10, symbol).orElseThrow();
        assertThat(position.getQty()).isEqualByComparingTo("1");
        assertThat(position.getMargin()).isEqualByComparingTo("5.000000000000000000");
        assertThat(fixture.accountRepo.findByUid(10).orElseThrow().crossBalance()).isEqualByComparingTo("1000");
        assertThat(fixture.ledgerRepo.findByUid(10)).extracting(WalletLedgerEntry::getReason)
                .contains("position_margin_release", "realized_pnl_profit", "adl_forced_loss");
        assertThat(fixture.published).filteredOn(AdlForcedDeleveragingRecorded.class::isInstance)
                .singleElement()
                .satisfies(event -> {
                    AdlForcedDeleveragingRecorded audit = (AdlForcedDeleveragingRecorded) event;
                    assertThat(audit.executed()).isTrue();
                    assertThat(audit.reason()).isEqualTo("EXECUTED");
                    assertThat(audit.socializedLossCharged()).isEqualByComparingTo("20");
                });
    }

    @Test
    @DisplayName("相同 ADL command id 重送不會重複減倉或重複寫 ledger")
    /**
     * 流程：同一 command id 執行兩次 -> 第二次直接回傳第一次結果，不再次異動 position 或 ledger。
     */
    void repeatedCommandIdIsIdempotent() {
        Fixture fixture = fixture();
        fixture.seedLongPosition(10, "2", "80", "10");

        AdlExecutionResult first = fixture.service.execute("adl-idempotent", plan("100", "1"));
        int ledgerEntriesAfterFirstRun = fixture.ledgerRepo.findByUid(10).size();
        AdlExecutionResult second = fixture.service.execute("adl-idempotent", plan("100", "1"));

        assertThat(second).isSameAs(first);
        assertThat(fixture.positionRepo.find(10, symbol).orElseThrow().getQty()).isEqualByComparingTo("1");
        assertThat(fixture.ledgerRepo.findByUid(10)).hasSize(ledgerEntriesAfterFirstRun);
        assertThat(fixture.published).filteredOn(AdlForcedDeleveragingRecorded.class::isInstance).hasSize(1);
    }

    @Test
    @DisplayName("durable ADL execution store 會接手 command id 去重")
    /**
     * 流程：第一次執行寫入 durable execution summary -> 模擬新 service instance 用同一 command id 重送 ->
     * 第二次只讀取 durable 結果，不重新 start、不改 position/ledger。
     */
    void durableExecutionStorePreventsDuplicateExecutionAcrossServiceInstances() {
        Fixture fixture = fixture();
        fixture.seedLongPosition(10, "2", "80", "10");
        MemAdlExecutionStore store = new MemAdlExecutionStore();
        fixture.service.setAdlExecutionStore(store);

        AdlExecutionResult first = fixture.service.execute("adl-durable", plan("100", "1"));
        int ledgerEntriesAfterFirstRun = fixture.ledgerRepo.findByUid(10).size();

        AdlForcedExecutionService restarted = new AdlForcedExecutionService(
                fixture.accountRepo,
                fixture.positionRepo,
                new DefaultSymbolConfigRepository(),
                fixture.walletLedgerService,
                fixture.published::add
        );
        restarted.setAdlExecutionStore(store);
        AdlExecutionResult second = restarted.execute("adl-durable", plan("100", "1"));

        assertThat(second.commandId()).isEqualTo(first.commandId());
        assertThat(second.executed()).isTrue();
        assertThat(second.executedNotional()).isEqualByComparingTo("100");
        assertThat(fixture.positionRepo.find(10, symbol).orElseThrow().getQty()).isEqualByComparingTo("1");
        assertThat(fixture.ledgerRepo.findByUid(10)).hasSize(ledgerEntriesAfterFirstRun);
        assertThat(store.startAttempts).isEqualTo(1);
        assertThat(store.completeCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("recentExecutions 會回傳最近 ADL forced execution outcome")
    /**
     * 流程：execution 完成後，後台報告 API 需要能取回最近 outcome；durable store 存在時以 store 為準。
     */
    void recentExecutionsReturnsLatestOutcomesFromStore() {
        Fixture fixture = fixture();
        fixture.seedLongPosition(10, "2", "80", "10");
        MemAdlExecutionStore store = new MemAdlExecutionStore();
        fixture.service.setAdlExecutionStore(store);

        AdlExecutionResult result = fixture.service.execute("adl-report-1", plan("100", "1"));

        assertThat(fixture.service.recentExecutions(10)).containsExactly(result);
        assertThatThrownBy(() -> fixture.service.recentExecutions(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 500");
    }

    @Test
    @DisplayName("候選持倉數量不足時 ADL execution 不做部分異動")
    /**
     * 流程：plan 要減 3 BTC，但候選只有 2 BTC -> validation 在寫 position/ledger 前失敗。
     */
    void insufficientCandidateQuantityRejectsBeforeMutation() {
        Fixture fixture = fixture();
        fixture.seedLongPosition(10, "2", "80", "10");
        int ledgerEntriesBefore = fixture.ledgerRepo.findByUid(10).size();

        assertThatThrownBy(() -> fixture.service.execute("adl-too-large", plan("300", "3")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("quantity is insufficient");

        assertThat(fixture.positionRepo.find(10, symbol).orElseThrow().getQty()).isEqualByComparingTo("2");
        assertThat(fixture.ledgerRepo.findByUid(10)).hasSize(ledgerEntriesBefore);
        assertThat(fixture.published).isEmpty();
    }

    @Test
    @DisplayName("operator halt 會拒絕 ADL execution 並留下 audit reason")
    /**
     * 流程：營運開啟 liquidation halt -> ADL executor 拒絕執行且發布 ADL_HALTED audit。
     */
    void operatorHaltPreventsExecutionAndPublishesAudit() {
        Fixture fixture = fixture();
        fixture.seedLongPosition(10, "2", "80", "10");
        RiskControlsProperties controls = new RiskControlsProperties();
        controls.setLiquidationHalt(true);
        fixture.service.setRiskControlsProperties(controls);

        assertThatThrownBy(() -> fixture.service.execute("adl-halted", plan("100", "1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("halted");

        assertThat(fixture.positionRepo.find(10, symbol).orElseThrow().getQty()).isEqualByComparingTo("2");
        assertThat(fixture.published).filteredOn(AdlForcedDeleveragingRecorded.class::isInstance)
                .singleElement()
                .satisfies(event -> {
                    AdlForcedDeleveragingRecorded audit = (AdlForcedDeleveragingRecorded) event;
                    assertThat(audit.executed()).isFalse();
                    assertThat(audit.reason()).isEqualTo("ADL_HALTED");
                });
    }

    private Fixture fixture() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        List<Object> published = new ArrayList<>();
        AdlForcedExecutionService service = new AdlForcedExecutionService(
                accountRepo,
                positionRepo,
                new DefaultSymbolConfigRepository(),
                walletLedgerService,
                published::add
        );
        return new Fixture(accountRepo, ledgerRepo, positionRepo, walletLedgerService, published, service);
    }

    private AdlDeleveragingPlan plan(String reduceNotional, String reduceQty) {
        return new AdlDeleveragingPlan(
                new BigDecimal(reduceNotional),
                new BigDecimal(reduceNotional),
                BigDecimal.ZERO,
                List.of(new AdlDeleveragingStep(
                        1,
                        10,
                        "BTCUSDT",
                        new BigDecimal(reduceNotional),
                        new BigDecimal(reduceQty)
                ))
        );
    }

    private record Fixture(
            MemAccountRepository accountRepo,
            MemWalletLedgerRepository ledgerRepo,
            MemPositionRepository positionRepo,
            WalletLedgerService walletLedgerService,
            List<Object> published,
            AdlForcedExecutionService service
    ) {
        void seedLongPosition(long uid, String qty, String entryPrice, String margin) {
            walletLedgerService.deposit(uid, "USDT", new BigDecimal("1000"), "deposit");
            walletLedgerService.increasePositionMargin(uid, "USDT", new BigDecimal(margin), "margin");
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

    private static class MemAdlExecutionStore implements AdlExecutionStore {
        private final Map<String, AdlExecutionResult> completed = new LinkedHashMap<>();
        private final List<String> started = new ArrayList<>();
        private int startAttempts;
        private int completeCalls;

        @Override
        public Optional<AdlExecutionResult> findCompleted(String commandId) {
            return Optional.ofNullable(completed.get(commandId));
        }

        @Override
        public List<AdlExecutionResult> findRecent(int limit) {
            return completed.values().stream()
                    .limit(limit)
                    .toList();
        }

        @Override
        public boolean tryStart(String commandId, AdlDeleveragingPlan plan, java.time.Instant startedAt) {
            startAttempts++;
            if (completed.containsKey(commandId) || started.contains(commandId)) {
                return false;
            }
            started.add(commandId);
            return true;
        }

        @Override
        public void complete(AdlExecutionResult result) {
            completeCalls++;
            completed.put(result.commandId(), result);
        }

        @Override
        public void reject(AdlExecutionResult result) {
            completed.put(result.commandId(), result);
        }
    }
}
