/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.FundingSettled;
import com.example.exchange.domain.event.LiquidationDecisionRecorded;
import com.example.exchange.domain.event.PositionLiquidated;
import com.example.exchange.domain.model.dto.FundingSettlementResult;
import com.example.exchange.domain.model.dto.LiquidationResult;
import com.example.exchange.domain.model.dto.LiquidationScanResult;
import com.example.exchange.domain.model.dto.ValidationIssue;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.enums.MarginMode;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import com.example.exchange.infra.config.DefaultSymbolConfigRepository;
import com.example.exchange.infra.config.FundingRateProperties;
import com.example.exchange.infra.config.MarkPriceOracleProperties;
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

/**
 * 測試 funding、liquidation 與 reconciliation 的風控結算流程。
 *
 * <p>這裡用 in-memory repository 驗證帳戶餘額、持倉、ledger、
 * domain event 與 reconciliation 結果是否一致。</p>
 */
class RiskSettlementServiceTest {

    private final Symbol symbol = Symbol.builder()
            .base("BTC")
            .quote("USDT")
            .priceScale(1)
            .qtyScale(3)
            .build();

    @Test
    @DisplayName("單一 funding settlement 會更新 ledger、position 並保持可對帳")
    /**
     * 流程：準備多單、帳戶與保證金 -> 單筆 settle funding -> 驗證 cashflow、ledger、event 與 reconciliation。
     */
    void fundingSettlementUpdatesLedgerPositionAndStillReconciles() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        List<FundingSettled> published = new ArrayList<>();

        walletLedgerService.deposit(1, "USDT", new BigDecimal("1000"), "deposit");
        walletLedgerService.increasePositionMargin(1, "USDT", new BigDecimal("5"), "margin");
        positionRepo.save(Position.builder()
                .uid(1)
                .symbol(symbol)
                .mode(MarginMode.CROSS)
                .leverage(new BigDecimal("20"))
                .qty(new BigDecimal("1"))
                .entryPrice(new BigDecimal("100"))
                .margin(new BigDecimal("5"))
                .build());

        FundingRateService fundingRateService = new FundingRateService(
                positionRepo,
                symbolRepo,
                walletLedgerService,
                published::add,
                new FundingRateProperties()
        );

        var result = fundingRateService.settle(
                1,
                "BTCUSDT",
                new BigDecimal("100"),
                new BigDecimal("0.01")
        );

        assertThat(result.settled()).isTrue();
        assertThat(result.cashflow()).isEqualByComparingTo("-1.00");
        assertThat(positionRepo.find(1, symbol).orElseThrow().getFundingPaid()).isEqualByComparingTo("1.00");
        assertThat(ledgerRepo.findByUid(1)).extracting(WalletLedgerEntry::getReason)
                .contains("funding_fee_paid");
        assertThat(published).hasSize(1);

        ReconciliationService reconciliationService = new ReconciliationService(accountRepo, positionRepo, ledgerRepo);
        assertThat(reconciliationService.validateUid(1)).isEmpty();
    }

    @Test
    @DisplayName("configured funding settlement 會掃描 open positions 並分別結算多空")
    /**
     * 流程：建立同 symbol 一多一空 -> 使用設定檔 settleConfiguredSymbols -> 驗證多方付費、空方收費。
     */
    void configuredFundingSettlementScansOpenPositions() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        List<FundingSettled> published = new ArrayList<>();

        walletLedgerService.deposit(1, "USDT", new BigDecimal("1000"), "deposit-1");
        walletLedgerService.deposit(2, "USDT", new BigDecimal("1000"), "deposit-2");
        positionRepo.save(Position.builder()
                .uid(1)
                .symbol(symbol)
                .mode(MarginMode.CROSS)
                .leverage(new BigDecimal("20"))
                .qty(new BigDecimal("1"))
                .entryPrice(new BigDecimal("100"))
                .build());
        positionRepo.save(Position.builder()
                .uid(2)
                .symbol(symbol)
                .mode(MarginMode.CROSS)
                .leverage(new BigDecimal("20"))
                .qty(new BigDecimal("-2"))
                .entryPrice(new BigDecimal("100"))
                .build());

        FundingRateService fundingRateService = new FundingRateService(
                positionRepo,
                symbolRepo,
                walletLedgerService,
                published::add,
                fundingRateProperties("BTCUSDT", "0.01")
        );
        fundingRateService.setMarkPriceOracleService(oracle("BTCUSDT", "100", "100"));

        var results = fundingRateService.settleConfiguredSymbols();

        assertThat(results).hasSize(2);
        assertThat(results).extracting(FundingSettlementResult::cashflow)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("-1.00"), new BigDecimal("2.00"));
        assertThat(published).hasSize(2);
        assertThat(positionRepo.find(1, symbol).orElseThrow().getFundingPaid()).isEqualByComparingTo("1.00");
        assertThat(positionRepo.find(2, symbol).orElseThrow().getFundingReceived()).isEqualByComparingTo("2.00");
    }

    @Test
    @DisplayName("強平會關閉倉位並用保險基金承接帳戶不足的缺口")
    /**
     * 流程：建立高虧損多單 -> 用極低 mark price 強平 -> 驗證倉位歸零、帳戶歸零與保險基金補缺口。
     */
    void liquidationClosesPositionAndUsesInsuranceFundForShortfall() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        InsuranceFundService insuranceFundService = new InsuranceFundService();
        List<Object> published = new ArrayList<>();

        // mark price 從 100 跌到 1，製造遠超帳戶餘額的虧損缺口。
        walletLedgerService.deposit(7, "USDT", new BigDecimal("10"), "deposit");
        walletLedgerService.increasePositionMargin(7, "USDT", new BigDecimal("5"), "margin");
        positionRepo.save(Position.builder()
                .uid(7)
                .symbol(symbol)
                .mode(MarginMode.CROSS)
                .leverage(new BigDecimal("20"))
                .qty(new BigDecimal("1"))
                .entryPrice(new BigDecimal("100"))
                .margin(new BigDecimal("5"))
                .build());

        LiquidationService liquidationService = new LiquidationService(
                accountRepo,
                positionRepo,
                symbolRepo,
                walletLedgerService,
                insuranceFundService,
                published::add
        );
        liquidationService.setMarkPriceOracleService(oracle("BTCUSDT", "1", "1"));

        LiquidationResult result = liquidationService.liquidate(7, "BTCUSDT");

        assertThat(result.liquidated()).isTrue();
        assertThat(result.closedQty()).isEqualByComparingTo("-1");
        assertThat(result.realizedPnl()).isEqualByComparingTo("-99");
        assertThat(result.insuranceFundCovered()).isEqualByComparingTo("89");
        assertThat(result.adlCovered()).isEqualByComparingTo("0");

        Position position = positionRepo.find(7, symbol).orElseThrow();
        assertThat(position.getQty()).isEqualByComparingTo("0");
        assertThat(position.getMargin()).isEqualByComparingTo("0");
        assertThat(position.getInsuranceFundCovered()).isEqualByComparingTo("89");
        assertThat(accountRepo.findByUid(7).orElseThrow().crossBalance()).isEqualByComparingTo("0");
        assertThat(ledgerRepo.findByUid(7)).extracting(WalletLedgerEntry::getReason)
                .contains("insurance_fund_payout", "realized_pnl_loss", "position_margin_release");
        assertThat(published).filteredOn(PositionLiquidated.class::isInstance).hasSize(1);
        assertThat(published).filteredOn(LiquidationDecisionRecorded.class::isInstance)
                .singleElement()
                .satisfies(event -> {
                    LiquidationDecisionRecorded decision = (LiquidationDecisionRecorded) event;
                    assertThat(decision.liquidated()).isTrue();
                    assertThat(decision.reason()).isEqualTo("EQUITY_BELOW_MAINTENANCE");
                    assertThat(decision.insuranceCovered()).isEqualByComparingTo("89");
                    assertThat(decision.adlCovered()).isEqualByComparingTo("0");
                });
        assertThat(insuranceFundService.movements("USDT", 10))
                .singleElement()
                .satisfies(movement -> {
                    assertThat(movement.reason()).isEqualTo("INSURANCE_FUND_PAYOUT");
                    assertThat(movement.refId()).isEqualTo(result.liquidationId());
                    assertThat(movement.amount()).isEqualByComparingTo("-89");
                });
        assertThat(insuranceFundService.adlQueue()).isEmpty();
    }

    @Test
    @DisplayName("liquidation manual review 只記錄 audit decision 不執行平倉")
    /**
     * 流程：建立會觸發強平的虧損倉位 -> 開啟 manual review ->
     * 驗證 liquidation 不平倉、不寫 ledger，只發布人工覆核 decision audit。
     */
    void liquidationManualReviewRecordsDecisionWithoutClosingPosition() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        InsuranceFundService insuranceFundService = new InsuranceFundService();
        List<Object> published = new ArrayList<>();
        RiskControlsProperties controls = new RiskControlsProperties();
        controls.setLiquidationManualReview(true);

        walletLedgerService.deposit(8, "USDT", new BigDecimal("10"), "deposit");
        walletLedgerService.increasePositionMargin(8, "USDT", new BigDecimal("5"), "margin");
        positionRepo.save(Position.builder()
                .uid(8)
                .symbol(symbol)
                .mode(MarginMode.CROSS)
                .leverage(new BigDecimal("20"))
                .qty(new BigDecimal("1"))
                .entryPrice(new BigDecimal("100"))
                .margin(new BigDecimal("5"))
                .build());

        LiquidationService liquidationService = new LiquidationService(
                accountRepo,
                positionRepo,
                symbolRepo,
                walletLedgerService,
                insuranceFundService,
                published::add
        );
        liquidationService.setRiskControlsProperties(controls);
        liquidationService.setMarkPriceOracleService(oracle("BTCUSDT", "1", "1"));

        LiquidationResult result = liquidationService.liquidate(8, "BTCUSDT");

        assertThat(result.liquidated()).isFalse();
        assertThat(positionRepo.find(8, symbol).orElseThrow().getQty()).isEqualByComparingTo("1");
        assertThat(ledgerRepo.findByUid(8)).extracting(WalletLedgerEntry::getReason)
                .doesNotContain("insurance_fund_payout", "realized_pnl_loss", "position_margin_release");
        assertThat(published).filteredOn(PositionLiquidated.class::isInstance).isEmpty();
        assertThat(published).filteredOn(LiquidationDecisionRecorded.class::isInstance)
                .singleElement()
                .satisfies(event -> {
                    LiquidationDecisionRecorded decision = (LiquidationDecisionRecorded) event;
                    assertThat(decision.liquidated()).isFalse();
                    assertThat(decision.reason()).isEqualTo("LIQUIDATION_MANUAL_REVIEW");
                });
    }

    @Test
    @DisplayName("liquidation scanner 會掃描 open positions 並觸發 oracle-based liquidation")
    /**
     * 流程：準備一筆會強平與一筆安全倉位 -> scanner 掃 open positions ->
     * 驗證只強平高風險倉位，安全倉位只留下 decision audit。
     */
    void liquidationScannerScansOpenPositionsAndTriggersLiquidation() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        InsuranceFundService insuranceFundService = new InsuranceFundService();
        List<Object> published = new ArrayList<>();

        walletLedgerService.deposit(41, "USDT", new BigDecimal("10"), "deposit-41");
        walletLedgerService.increasePositionMargin(41, "USDT", new BigDecimal("5"), "margin-41");
        positionRepo.save(Position.builder()
                .uid(41)
                .symbol(symbol)
                .mode(MarginMode.CROSS)
                .leverage(new BigDecimal("20"))
                .qty(new BigDecimal("1"))
                .entryPrice(new BigDecimal("100"))
                .margin(new BigDecimal("5"))
                .build());

        walletLedgerService.deposit(42, "USDT", new BigDecimal("1000"), "deposit-42");
        walletLedgerService.increasePositionMargin(42, "USDT", new BigDecimal("5"), "margin-42");
        positionRepo.save(Position.builder()
                .uid(42)
                .symbol(symbol)
                .mode(MarginMode.CROSS)
                .leverage(new BigDecimal("2"))
                .qty(new BigDecimal("1"))
                .entryPrice(new BigDecimal("1"))
                .margin(new BigDecimal("5"))
                .build());

        LiquidationService liquidationService = new LiquidationService(
                accountRepo,
                positionRepo,
                symbolRepo,
                walletLedgerService,
                insuranceFundService,
                published::add
        );
        liquidationService.setMarkPriceOracleService(oracle("BTCUSDT", "1", "1"));
        LiquidationScanService scanService = new LiquidationScanService(positionRepo, liquidationService);

        LiquidationScanResult scan = scanService.scanOpenPositions();

        assertThat(scan.scannedPositions()).isEqualTo(2);
        assertThat(scan.liquidationCount()).isEqualTo(1);
        assertThat(scan.reviewedCount()).isEqualTo(1);
        assertThat(positionRepo.find(41, symbol).orElseThrow().getQty()).isEqualByComparingTo("0");
        assertThat(positionRepo.find(42, symbol).orElseThrow().getQty()).isEqualByComparingTo("1");
        assertThat(published).filteredOn(LiquidationDecisionRecorded.class::isInstance).hasSize(2);
        assertThat(published).filteredOn(PositionLiquidated.class::isInstance).hasSize(1);
    }

    @Test
    @DisplayName("liquidation scanner 支援 batch limit 且單筆失敗不會中止整批")
    /**
     * 流程：三筆 open positions，scan batch size 設為 2，第一筆因缺 symbol config 失敗，第二筆安全倉位正常判斷。
     * 期望：scanner 只處理兩筆；第一筆被計入 reviewed，不阻止第二筆產生 decision audit。
     */
    void liquidationScannerLimitsBatchAndContinuesAfterPositionFailure() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        InsuranceFundService insuranceFundService = new InsuranceFundService();
        List<Object> published = new ArrayList<>();
        RiskControlsProperties controls = new RiskControlsProperties();
        controls.setLiquidationScanBatchSize(2);

        positionRepo.save(Position.builder()
                .uid(51)
                .symbol(Symbol.builder().base("DOGE").quote("USDT").priceScale(1).qtyScale(1).build())
                .mode(MarginMode.CROSS)
                .qty(new BigDecimal("1"))
                .entryPrice(new BigDecimal("1"))
                .build());

        walletLedgerService.deposit(52, "USDT", new BigDecimal("1000"), "deposit-52");
        positionRepo.save(Position.builder()
                .uid(52)
                .symbol(symbol)
                .mode(MarginMode.CROSS)
                .qty(new BigDecimal("1"))
                .entryPrice(new BigDecimal("1"))
                .build());

        positionRepo.save(Position.builder()
                .uid(53)
                .symbol(symbol)
                .mode(MarginMode.CROSS)
                .qty(new BigDecimal("1"))
                .entryPrice(new BigDecimal("100"))
                .build());

        LiquidationService liquidationService = new LiquidationService(
                accountRepo,
                positionRepo,
                symbolRepo,
                walletLedgerService,
                insuranceFundService,
                published::add
        );
        liquidationService.setMarkPriceOracleService(oracle("BTCUSDT", "1", "1"));
        LiquidationScanService scanService = new LiquidationScanService(positionRepo, liquidationService);
        scanService.setRiskControlsProperties(controls);

        LiquidationScanResult scan = scanService.scanOpenPositions();

        assertThat(scan.scannedPositions()).isEqualTo(2);
        assertThat(scan.liquidationCount()).isZero();
        assertThat(scan.reviewedCount()).isEqualTo(2);
        assertThat(scan.results()).hasSize(1);
        assertThat(published).filteredOn(LiquidationDecisionRecorded.class::isInstance).hasSize(1);
        assertThat(positionRepo.find(53, symbol).orElseThrow().getQty()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("全帳戶對帳會掃 account index 與 open-position index 找出不一致")
    /**
     * 流程：準備正常帳戶、margin 不一致帳戶、缺 account 的 open position -> validateAllAccounts 找出兩種問題。
     */
    void reconciliationCanScanKnownAccountsAndOpenPositionAccounts() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();

        Account balanced = new Account(31);
        balanced.deposit(new BigDecimal("100"));
        accountRepo.save(balanced);

        Account missingPositionMargin = new Account(32);
        missingPositionMargin.deposit(new BigDecimal("100"));
        missingPositionMargin.reservePositionMargin(new BigDecimal("10"));
        accountRepo.save(missingPositionMargin);

        // uid=33 故意只有 open position 沒有 account，驗證掃 open-position index 能抓到缺帳戶。
        positionRepo.save(Position.builder()
                .uid(33)
                .symbol(symbol)
                .mode(MarginMode.CROSS)
                .leverage(new BigDecimal("20"))
                .qty(new BigDecimal("1"))
                .entryPrice(new BigDecimal("100"))
                .margin(new BigDecimal("5"))
                .build());

        List<ValidationIssue> issues = new ReconciliationService(accountRepo, positionRepo, ledgerRepo)
                .validateAllAccounts();

        assertThat(issues).extracting(ValidationIssue::code)
                .containsExactly("POSITION_MARGIN_MISMATCH", "ACCOUNT_MISSING");
    }

    private static class MemAccountRepository implements AccountRepository {
        private final Map<Long, Account> accounts = new LinkedHashMap<>();

        @Override
        /**
         * 依 uid 找帳戶，支援 funding、liquidation 與 reconciliation 讀取帳戶狀態。
         */
        public Optional<Account> findByUid(long uid) {
            return Optional.ofNullable(accounts.get(uid));
        }

        @Override
        /**
         * 回傳全部已知帳戶，讓全帳戶對帳能掃 account index。
         */
        public List<Account> findAll() {
            return new ArrayList<>(accounts.values());
        }

        @Override
        /**
         * 保存帳戶餘額與保證金狀態，模擬 repository 寫回。
         */
        public void save(Account account) {
            accounts.put(account.uid(), account);
        }
    }

    private static class MemWalletLedgerRepository implements WalletLedgerRepository {
        private final List<WalletLedgerEntry> entries = new ArrayList<>();

        @Override
        /**
         * 追加 ledger entry，並在 stub 層先驗證雙分錄平衡。
         */
        public void append(WalletLedgerEntry entry) {
            assertThat(entry.isBalanced()).isTrue();
            entries.add(entry);
        }

        @Override
        /**
         * 依 uid 查 ledger，用於驗證 funding、liquidation 是否寫入正確 reason。
         */
        public List<WalletLedgerEntry> findByUid(long uid) {
            return entries.stream().filter(entry -> entry.getUid() == uid).toList();
        }

        @Override
        /**
         * 依 refId 查 ledger；reconciliation path 保留此 contract 以避免 stub 行為缺口。
         */
        public List<WalletLedgerEntry> findByRefId(String refId) {
            return entries.stream().filter(entry -> refId.equals(entry.getRefId())).toList();
        }
    }

    private static class MemPositionRepository implements PositionRepository {
        private final Map<String, Position> positions = new LinkedHashMap<>();

        @Override
        /**
         * 依 uid + symbol 查單一持倉，funding 與 liquidation 都從這裡取得目標部位。
         */
        public Optional<Position> find(long uid, Symbol symbol) {
            return Optional.ofNullable(positions.get(key(uid, symbol.code())));
        }

        @Override
        /**
         * 保存持倉變化，包含 funding paid/received、強平後 qty 與 margin 更新。
         */
        public void save(Position position) {
            positions.put(key(position.getUid(), position.getSymbol().code()), position);
        }

        @Override
        /**
         * 回傳指定 uid 的持倉集合，reconciliation 用來比對帳戶保證金與 position margin。
         */
        public List<Position> findAllByUid(long uid) {
            return positions.values().stream().filter(position -> position.getUid() == uid).toList();
        }

        @Override
        /**
         * 回傳所有 open positions，configured funding 與全帳戶對帳都會掃這個集合。
         */
        public List<Position> findOpenPositions() {
            return positions.values().stream()
                    .filter(position -> position.getQty() != null && position.getQty().signum() != 0)
                    .toList();
        }

        /**
         * 建立 uid + symbol 複合 key，避免不同帳戶或不同市場的 position 覆蓋。
         */
        private static String key(long uid, String symbol) {
            return uid + ":" + symbol;
        }
    }

    /**
     * 建立 funding rate 設定物件，讓 configured settlement 測試可走與 production 相同的配置入口。
     */
    private static FundingRateProperties fundingRateProperties(String symbol, String fundingRate) {
        FundingRateProperties properties = new FundingRateProperties();
        FundingRateProperties.Settlement settlement = new FundingRateProperties.Settlement();
        settlement.setSymbol(symbol);
        settlement.setFundingRate(new BigDecimal(fundingRate));
        properties.setSettlements(List.of(settlement));
        return properties;
    }

    /**
     * 建立測試用 mark/index price oracle，模擬 production 由獨立價格來源餵價。
     */
    private static MarkPriceOracleService oracle(String symbol, String markPrice, String indexPrice) {
        MarkPriceOracleProperties properties = new MarkPriceOracleProperties();
        MarkPriceOracleService oracle = new MarkPriceOracleService(properties);
        oracle.update(symbol, new BigDecimal(markPrice), new BigDecimal(indexPrice), "test");
        return oracle;
    }
}
