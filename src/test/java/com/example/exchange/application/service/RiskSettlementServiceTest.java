package com.example.exchange.application.service;

import com.example.exchange.domain.event.FundingSettled;
import com.example.exchange.domain.event.PositionLiquidated;
import com.example.exchange.domain.model.dto.LiquidationResult;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.enums.MarginMode;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import com.example.exchange.infra.config.DefaultSymbolConfigRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RiskSettlementServiceTest {

    private final Symbol symbol = Symbol.builder()
            .base("BTC")
            .quote("USDT")
            .priceScale(1)
            .qtyScale(3)
            .build();

    @Test
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
                published::add
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
    void liquidationClosesPositionAndUsesInsuranceFundForShortfall() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        InsuranceFundService insuranceFundService = new InsuranceFundService();
        List<PositionLiquidated> published = new ArrayList<>();

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

        LiquidationResult result = liquidationService.liquidate(7, "BTCUSDT", new BigDecimal("1"));

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
        assertThat(published).hasSize(1);
        assertThat(insuranceFundService.adlQueue()).isEmpty();
    }

    private static class MemAccountRepository implements AccountRepository {
        private final Map<Long, Account> accounts = new LinkedHashMap<>();

        @Override
        public Optional<Account> findByUid(long uid) {
            return Optional.ofNullable(accounts.get(uid));
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

        private static String key(long uid, String symbol) {
            return uid + ":" + symbol;
        }
    }
}
