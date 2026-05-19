/*
 * 檔案用途：測試 durable wallet ledger replay 與 account comparison。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.WalletLedgerReplayResult;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletLedgerPosting;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.WalletLedgerJournal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WalletLedgerReplayServiceTest {

    @Test
    @DisplayName("replay 會由 USER_* postings 重建 available、hold、position margin 與 balance")
    /**
     * 流程：入金 -> 委託預凍 -> 從 hold 收費，然後用 durable journal replay 重建帳戶狀態。
     */
    void replayRebuildsCrossBalancesFromUserPostings() {
        MemWalletLedgerJournal journal = new MemWalletLedgerJournal();
        journal.append(entry("deposit", new BigDecimal("100"), List.of(
                debit("USER_AVAILABLE", "USDT", "100"),
                credit("EXTERNAL_CASH", "USDT", "100")
        )));
        journal.append(entry("order_reserve", new BigDecimal("30"), List.of(
                debit("USER_ORDER_HOLD", "USDT", "30"),
                credit("USER_AVAILABLE", "USDT", "30")
        )));
        journal.append(entry("trade_fee", new BigDecimal("5"), List.of(
                debit("USER_FEE_EXPENSE", "USDT", "5"),
                credit("USER_ORDER_HOLD", "USDT", "5")
        )));

        WalletLedgerReplayResult result = new WalletLedgerReplayService(
                journal,
                new MemAccountRepository()
        ).replay(7, "USDT");

        assertThat(result.entryCount()).isEqualTo(3);
        assertThat(result.postingCount()).isEqualTo(6);
        assertThat(result.available()).isEqualByComparingTo("70");
        assertThat(result.orderHold()).isEqualByComparingTo("25");
        assertThat(result.positionMargin()).isEqualByComparingTo("0");
        assertThat(result.balance()).isEqualByComparingTo("95");
        assertThat(result.balanced()).isTrue();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    @DisplayName("replayAndCompareAccount 會回報 durable ledger 與現行 account 是否一致")
    /**
     * 流程：journal replay 得到 95/70/25/0，account 也 restore 成相同狀態，驗證 comparison 無 issue。
     */
    void replayAndCompareAccountReportsNoIssueWhenBalancesMatch() {
        MemWalletLedgerJournal journal = new MemWalletLedgerJournal();
        journal.append(entry("deposit", new BigDecimal("100"), List.of(
                debit("USER_AVAILABLE", "USDT", "100"),
                credit("EXTERNAL_CASH", "USDT", "100")
        )));
        journal.append(entry("order_reserve", new BigDecimal("30"), List.of(
                debit("USER_ORDER_HOLD", "USDT", "30"),
                credit("USER_AVAILABLE", "USDT", "30")
        )));
        journal.append(entry("trade_fee", new BigDecimal("5"), List.of(
                debit("USER_FEE_EXPENSE", "USDT", "5"),
                credit("USER_ORDER_HOLD", "USDT", "5")
        )));
        Account account = new Account(7);
        account.restoreCross(new BigDecimal("95"), new BigDecimal("70"), new BigDecimal("25"), BigDecimal.ZERO);

        WalletLedgerReplayResult result = new WalletLedgerReplayService(
                journal,
                new MemAccountRepository(account)
        ).replayAndCompareAccount(7, "USDT");

        assertThat(result.balanced()).isTrue();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    @DisplayName("replay 會標記不平衡 ledger entry")
    /**
     * 流程：放入只有 debit、沒有對應 credit 的 entry，驗證 replay issue 包含 UNBALANCED_ENTRY。
     */
    void replayFlagsUnbalancedEntry() {
        MemWalletLedgerJournal journal = new MemWalletLedgerJournal();
        journal.entries.add(entry("broken", new BigDecimal("10"), List.of(
                debit("USER_AVAILABLE", "USDT", "10")
        )));

        WalletLedgerReplayResult result = new WalletLedgerReplayService(
                journal,
                new MemAccountRepository()
        ).replay(7, "USDT");

        assertThat(result.balanced()).isFalse();
        assertThat(result.issues()).anyMatch(issue -> issue.startsWith("UNBALANCED_ENTRY:"));
    }

    private static WalletLedgerEntry entry(String reason, BigDecimal amount, List<WalletLedgerPosting> postings) {
        return WalletLedgerEntry.builder()
                .uid(7)
                .asset("USDT")
                .reason(reason)
                .refId(reason + "-ref")
                .amount(amount)
                .balanceAfter(BigDecimal.ZERO)
                .postings(postings)
                .build();
    }

    private static WalletLedgerPosting debit(String accountCode, String asset, String amount) {
        return new WalletLedgerPosting(accountCode, asset, new BigDecimal(amount), BigDecimal.ZERO);
    }

    private static WalletLedgerPosting credit(String accountCode, String asset, String amount) {
        return new WalletLedgerPosting(accountCode, asset, BigDecimal.ZERO, new BigDecimal(amount));
    }

    private static class MemWalletLedgerJournal implements WalletLedgerJournal {
        private final List<WalletLedgerEntry> entries = new ArrayList<>();

        @Override
        public void append(WalletLedgerEntry entry) {
            WalletLedgerJournal.validateBalancedEntry(entry);
            entries.add(entry);
        }

        @Override
        public List<WalletLedgerEntry> findByUid(long uid) {
            return entries.stream()
                    .filter(entry -> entry.getUid() == uid)
                    .toList();
        }

        @Override
        public List<WalletLedgerEntry> findByUidAndAsset(long uid, String asset) {
            return entries.stream()
                    .filter(entry -> entry.getUid() == uid)
                    .filter(entry -> asset.equals(entry.getAsset()))
                    .toList();
        }

        @Override
        public List<WalletLedgerEntry> findByRefId(String refId) {
            return entries.stream()
                    .filter(entry -> refId.equals(entry.getRefId()))
                    .toList();
        }
    }

    private static class MemAccountRepository implements AccountRepository {
        private final Account account;

        private MemAccountRepository() {
            this(null);
        }

        private MemAccountRepository(Account account) {
            this.account = account;
        }

        @Override
        public Optional<Account> findByUid(long uid) {
            return account == null || account.uid() != uid ? Optional.empty() : Optional.of(account);
        }

        @Override
        public void save(Account account) {
        }
    }
}
