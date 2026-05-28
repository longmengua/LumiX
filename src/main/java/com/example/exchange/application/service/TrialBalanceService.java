/*
 * 檔案用途：應用服務，從 wallet ledger postings 產生 trial balance。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.TrialBalanceLine;
import com.example.exchange.domain.model.dto.TrialBalanceReport;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletLedgerPosting;
import com.example.exchange.domain.repository.WalletLedgerJournal;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrialBalanceService {

    private final WalletLedgerRepository ledgerRepository;
    private WalletLedgerJournal ledgerJournal;

    @Autowired(required = false)
    public void setLedgerJournal(WalletLedgerJournal ledgerJournal) {
        this.ledgerJournal = ledgerJournal;
    }

    public TrialBalanceReport calculateForUid(long uid, String asset) {
        List<WalletLedgerEntry> entries = ledgerJournal == null
                ? ledgerRepository.findByUid(uid)
                : ledgerJournal.findByUidAndAsset(uid, asset);
        return calculate(uid, asset, entries);
    }

    public TrialBalanceReport calculate(long uid, String asset, List<WalletLedgerEntry> entries) {
        Map<Key, Totals> totals = new LinkedHashMap<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (WalletLedgerEntry entry : entries) {
            if (entry == null || entry.getPostings() == null) continue;
            if (asset != null && !asset.equals(entry.getAsset())) continue;
            for (WalletLedgerPosting posting : entry.getPostings()) {
                if (asset != null && !asset.equals(posting.asset())) continue;
                Key key = new Key(posting.asset(), posting.accountCode());
                Totals line = totals.computeIfAbsent(key, ignored -> new Totals());
                line.debit = line.debit.add(posting.debit());
                line.credit = line.credit.add(posting.credit());
                totalDebit = totalDebit.add(posting.debit());
                totalCredit = totalCredit.add(posting.credit());
            }
        }

        List<TrialBalanceLine> lines = new ArrayList<>();
        for (Map.Entry<Key, Totals> entry : totals.entrySet()) {
            BigDecimal net = entry.getValue().debit.subtract(entry.getValue().credit);
            lines.add(new TrialBalanceLine(
                    entry.getKey().asset,
                    entry.getKey().accountCode,
                    entry.getValue().debit,
                    entry.getValue().credit,
                    net.signum() > 0 ? net : BigDecimal.ZERO,
                    net.signum() < 0 ? net.abs() : BigDecimal.ZERO
            ));
        }
        lines.sort(Comparator
                .comparing(TrialBalanceLine::asset)
                .thenComparing(TrialBalanceLine::accountCode));

        return new TrialBalanceReport(
                uid,
                asset,
                totalDebit,
                totalCredit,
                totalDebit.compareTo(totalCredit) == 0,
                lines
        );
    }

    private record Key(String asset, String accountCode) {
    }

    private static class Totals {
        private BigDecimal debit = BigDecimal.ZERO;
        private BigDecimal credit = BigDecimal.ZERO;
    }
}
