/*
 * 檔案用途：財務日報 DTO，彙總 durable wallet ledger journal。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record FinanceDailyReport(
        LocalDate reportDate,
        Instant fromInclusive,
        Instant toExclusive,
        long entryCount,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean balanced,
        Instant generatedAt,
        List<FinanceDailyReportLine> lines
) {
    public FinanceDailyReport {
        totalDebit = totalDebit == null ? BigDecimal.ZERO : totalDebit;
        totalCredit = totalCredit == null ? BigDecimal.ZERO : totalCredit;
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
