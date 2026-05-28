/*
 * 檔案用途：測試 WalletLedgerService 的 bonus credit 獨立帳務。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WalletLedgerServiceTest {

    @Test
    @DisplayName("bonus credit grant/consume/expire 不會混入 real cash balance")
    void bonusCreditLifecycleIsAuditableAndSeparateFromRealCash() {
        MemAccountRepository accountRepository = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepository = new MemWalletLedgerRepository();
        WalletLedgerService service = new WalletLedgerService(accountRepository, ledgerRepository);

        // 流程：先入真實資金，再發體驗金；consume/expire 只改 bonus ledger，不改 Account cash。
        service.deposit(31, "USDT", new BigDecimal("100.00"), "cash-deposit");
        service.grantBonusCredit(31, "USDT", new BigDecimal("25.00"), "campaign-1");
        BigDecimal consumed = service.consumeBonusCredit(31, "USDT", new BigDecimal("7.50"), "fee-ref", "USER_FEE_EXPENSE");
        BigDecimal expired = service.expireBonusCredit(31, "USDT", new BigDecimal("99.00"), "campaign-expire");

        assertThat(consumed).isEqualByComparingTo("7.50");
        assertThat(expired).isEqualByComparingTo("17.50");
        assertThat(service.bonusCreditBalance(31, "USDT")).isEqualByComparingTo("0.00");
        assertThat(accountRepository.findByUid(31).orElseThrow().crossBalance()).isEqualByComparingTo("100.00");
        assertThat(ledgerRepository.findByUid(31))
                .extracting(WalletLedgerEntry::getReason)
                .containsExactly(
                        "deposit",
                        "bonus_credit_grant",
                        "bonus_credit_consume",
                        "bonus_credit_expire"
                );
    }

    @Test
    @DisplayName("clawback bonus credit 只回收剩餘可用體驗金")
    void clawbackBonusCreditCapsAtAvailableBalance() {
        MemWalletLedgerRepository ledgerRepository = new MemWalletLedgerRepository();
        WalletLedgerService service = new WalletLedgerService(new MemAccountRepository(), ledgerRepository);

        // 流程：發 10、先消耗 4，再 clawback 99；回收量必須封頂在剩餘 6。
        service.grantBonusCredit(32, "USDT", new BigDecimal("10.00"), "campaign-2");
        service.consumeBonusCredit(32, "USDT", new BigDecimal("4.00"), "fee-ref", "USER_FEE_EXPENSE");
        BigDecimal clawedBack = service.clawbackBonusCredit(32, "USDT", new BigDecimal("99.00"), "abuse-review");

        assertThat(clawedBack).isEqualByComparingTo("6.00");
        assertThat(service.bonusCreditBalance(32, "USDT")).isEqualByComparingTo("0.00");
        assertThat(ledgerRepository.findByUid(32).getLast().getPostings())
                .anyMatch(posting -> WalletLedgerService.BONUS_CLAWBACK_RECEIVABLE.equals(posting.accountCode()));
    }

    private static class MemAccountRepository implements AccountRepository {
        private Account account;

        @Override
        public Optional<Account> findByUid(long uid) {
            return account == null || account.uid() != uid ? Optional.empty() : Optional.of(account);
        }

        @Override
        public void save(Account account) {
            this.account = account;
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
