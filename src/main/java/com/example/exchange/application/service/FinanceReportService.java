/*
 * 檔案用途：應用服務，從 durable wallet ledger journal 產生財務報表。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.FinanceDailyReport;
import com.example.exchange.domain.model.dto.FinanceDailyReportLine;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletLedgerPosting;
import com.example.exchange.domain.repository.WalletLedgerJournal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinanceReportService {

    private final WalletLedgerJournal ledgerJournal;
    private final Clock clock = Clock.systemUTC();

    public FinanceDailyReport dailyReport(LocalDate reportDate) {
        LocalDate date = reportDate == null ? LocalDate.now(clock) : reportDate;
        Instant from = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return dailyReport(date, from, to, ledgerJournal.findByCreatedAtBetween(from, to));
    }

    FinanceDailyReport dailyReport(LocalDate date, Instant from, Instant to, List<WalletLedgerEntry> entries) {
        Map<Key, Totals> totalsByLine = new LinkedHashMap<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (WalletLedgerEntry entry : entries == null ? List.<WalletLedgerEntry>of() : entries) {
            if (entry == null || entry.getPostings() == null) continue;
            for (WalletLedgerPosting posting : entry.getPostings()) {
                Key key = new Key(entry.getReason(), posting.asset(), posting.accountCode());
                Totals totals = totalsByLine.computeIfAbsent(key, ignored -> new Totals());
                totals.debit = totals.debit.add(posting.debit());
                totals.credit = totals.credit.add(posting.credit());
                totalDebit = totalDebit.add(posting.debit());
                totalCredit = totalCredit.add(posting.credit());
            }
        }
        List<FinanceDailyReportLine> lines = new ArrayList<>();
        for (Map.Entry<Key, Totals> entry : totalsByLine.entrySet()) {
            lines.add(new FinanceDailyReportLine(
                    entry.getKey().reason,
                    entry.getKey().asset,
                    entry.getKey().accountCode,
                    entry.getValue().debit,
                    entry.getValue().credit,
                    null,
                    null
            ));
        }
        lines.sort(Comparator
                .comparing(FinanceDailyReportLine::reason)
                .thenComparing(FinanceDailyReportLine::asset)
                .thenComparing(FinanceDailyReportLine::accountCode));
        return new FinanceDailyReport(
                date,
                from,
                to,
                entries == null ? 0 : entries.size(),
                totalDebit,
                totalCredit,
                totalDebit.compareTo(totalCredit) == 0,
                clock.instant(),
                lines
        );
    }

    private record Key(String reason, String asset, String accountCode) {
    }

    private static class Totals {
        private BigDecimal debit = BigDecimal.ZERO;
        private BigDecimal credit = BigDecimal.ZERO;
    }
}
