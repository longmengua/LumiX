/*
 * 檔案用途：trial balance 每日快照 DTO，保存財務日結可重查的固定結果。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TrialBalanceSnapshot(
        LocalDate reportDate,
        long uid,
        String asset,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean balanced,
        Instant generatedAt,
        List<TrialBalanceLine> lines
) {
    public TrialBalanceSnapshot {
        totalDebit = totalDebit == null ? BigDecimal.ZERO : totalDebit;
        totalCredit = totalCredit == null ? BigDecimal.ZERO : totalCredit;
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public static TrialBalanceSnapshot from(LocalDate reportDate, TrialBalanceReport report) {
        return new TrialBalanceSnapshot(
                reportDate,
                report.uid(),
                report.asset(),
                report.totalDebit(),
                report.totalCredit(),
                report.balanced(),
                Instant.now(),
                report.lines()
        );
    }
}
