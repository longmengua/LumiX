/*
 * 檔案用途：測試 TrialBalanceService 從 ledger postings 產生 trial balance。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.TrialBalanceReport;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletLedgerPosting;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrialBalanceServiceTest {

    @Test
    @DisplayName("calculateForUid 會按 asset/accountCode 彙總借貸並確認 trial balance 平衡")
    void calculateForUidAggregatesPostingTotals() {
        MemWalletLedgerRepository ledgerRepository = new MemWalletLedgerRepository();
        ledgerRepository.entries.add(entry("deposit", List.of(
                debit("USER_AVAILABLE", "USDT", "100"),
                credit("EXTERNAL_CASH", "USDT", "100")
        )));
        ledgerRepository.entries.add(entry("fee", List.of(
                debit("USER_FEE_EXPENSE", "USDT", "3"),
                credit("USER_AVAILABLE", "USDT", "3")
        )));
        TrialBalanceService service = new TrialBalanceService(ledgerRepository);

        // 流程：兩筆 balanced ledger entry 進 trial balance，合計 debit/credit 必須相等。
        TrialBalanceReport report = service.calculateForUid(71, "USDT");

        assertThat(report.balanced()).isTrue();
        assertThat(report.totalDebit()).isEqualByComparingTo("103");
        assertThat(report.totalCredit()).isEqualByComparingTo("103");
        assertThat(report.lines()).anySatisfy(line -> {
            assertThat(line.accountCode()).isEqualTo("USER_AVAILABLE");
            assertThat(line.debit()).isEqualByComparingTo("100");
            assertThat(line.credit()).isEqualByComparingTo("3");
            assertThat(line.netDebit()).isEqualByComparingTo("97");
        });
    }

    @Test
    @DisplayName("calculate 會回報不平衡 postings 的 total debit/credit 差異")
    void calculateFlagsUnbalancedPostingSet() {
        TrialBalanceService service = new TrialBalanceService(new MemWalletLedgerRepository());

        // 流程：直接輸入缺少 credit 的 entry，trial balance 必須標示不平衡，供 reconciliation issue 分類。
        TrialBalanceReport report = service.calculate(72, "USDT", List.of(entry("broken", List.of(
                debit("USER_AVAILABLE", "USDT", "10")
        ))));

        assertThat(report.balanced()).isFalse();
        assertThat(report.totalDebit()).isEqualByComparingTo("10");
        assertThat(report.totalCredit()).isEqualByComparingTo("0");
    }

    private static WalletLedgerEntry entry(String reason, List<WalletLedgerPosting> postings) {
        return WalletLedgerEntry.builder()
                .uid(71)
                .asset("USDT")
                .reason(reason)
                .refId(reason + "-ref")
                .amount(BigDecimal.ZERO)
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

    private static class MemWalletLedgerRepository implements WalletLedgerRepository {
        private final List<WalletLedgerEntry> entries = new ArrayList<>();

        @Override
        public void append(WalletLedgerEntry entry) {
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
