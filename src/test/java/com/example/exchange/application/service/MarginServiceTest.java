package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletTransfer;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import com.example.exchange.domain.repository.WalletTransferRepository;
import com.example.exchange.infra.config.RiskControlsProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MarginServiceTest {

    @Test
    void depositAndWithdrawCreateTransfersAndLedgerEntries() {
        Fixtures fixtures = fixtures(new RiskControlsProperties());

        WalletTransfer deposit = fixtures.marginService.deposit(11, new BigDecimal("100.00"));
        WalletTransfer withdrawal = fixtures.marginService.withdraw(11, new BigDecimal("40.00"));

        Account account = fixtures.accountRepository.findByUid(11).orElseThrow();
        assertThat(deposit.getStatus()).isEqualTo(WalletTransfer.Status.CONFIRMED);
        assertThat(withdrawal.getStatus()).isEqualTo(WalletTransfer.Status.CONFIRMED);
        assertThat(account.crossBalance()).isEqualByComparingTo("60.00");
        assertThat(account.crossAvailable()).isEqualByComparingTo("60.00");
        assertThat(fixtures.ledgerRepository.findByUid(11))
                .extracting(WalletLedgerEntry::getReason)
                .containsExactly("deposit", "withdrawal");
        assertThat(fixtures.transferRepository.findByUid(11))
                .extracting(WalletTransfer::getType)
                .containsExactly(WalletTransfer.Type.DEPOSIT, WalletTransfer.Type.WITHDRAWAL);
    }

    @Test
    void withdrawalHaltRoutesToManualReviewWithoutDebiting() {
        RiskControlsProperties riskControls = new RiskControlsProperties();
        Fixtures fixtures = fixtures(riskControls);
        fixtures.marginService.deposit(12, new BigDecimal("100.00"));
        riskControls.setWithdrawalHalt(true);

        WalletTransfer withdrawal = fixtures.marginService.withdraw(12, new BigDecimal("25.00"));

        Account account = fixtures.accountRepository.findByUid(12).orElseThrow();
        assertThat(withdrawal.getStatus()).isEqualTo(WalletTransfer.Status.MANUAL_REVIEW);
        assertThat(withdrawal.getReason()).isEqualTo("WITHDRAWAL_HALTED");
        assertThat(account.crossBalance()).isEqualByComparingTo("100.00");
        assertThat(fixtures.ledgerRepository.findByUid(12))
                .extracting(WalletLedgerEntry::getReason)
                .containsExactly("deposit");
    }

    @Test
    void insufficientBalanceCreatesFailedWithdrawalWithoutLedgerEntry() {
        Fixtures fixtures = fixtures(new RiskControlsProperties());

        WalletTransfer withdrawal = fixtures.marginService.withdraw(13, new BigDecimal("10.00"));

        assertThat(withdrawal.getStatus()).isEqualTo(WalletTransfer.Status.FAILED);
        assertThat(withdrawal.getReason()).isEqualTo("INSUFFICIENT_AVAILABLE_BALANCE");
        assertThat(fixtures.accountRepository.findByUid(13)).isEmpty();
        assertThat(fixtures.ledgerRepository.findByUid(13)).isEmpty();
        assertThat(fixtures.transferRepository.findByUid(13)).containsExactly(withdrawal);
    }

    private static Fixtures fixtures(RiskControlsProperties riskControlsProperties) {
        MemAccountRepository accountRepository = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepository = new MemWalletLedgerRepository();
        MemWalletTransferRepository transferRepository = new MemWalletTransferRepository();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepository, ledgerRepository);
        MarginService marginService = new MarginService(
                accountRepository,
                walletLedgerService,
                ledgerRepository,
                transferRepository,
                riskControlsProperties
        );
        return new Fixtures(accountRepository, ledgerRepository, transferRepository, marginService);
    }

    private record Fixtures(
            MemAccountRepository accountRepository,
            MemWalletLedgerRepository ledgerRepository,
            MemWalletTransferRepository transferRepository,
            MarginService marginService
    ) {
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

    private static class MemWalletTransferRepository implements WalletTransferRepository {
        private final Map<UUID, WalletTransfer> transfers = new LinkedHashMap<>();

        @Override
        public void save(WalletTransfer transfer) {
            transfers.put(transfer.getId(), transfer);
        }

        @Override
        public Optional<WalletTransfer> findById(UUID id) {
            return Optional.ofNullable(transfers.get(id));
        }

        @Override
        public List<WalletTransfer> findByUid(long uid) {
            return transfers.values().stream()
                    .filter(transfer -> transfer.getUid() == uid)
                    .toList();
        }
    }
}
