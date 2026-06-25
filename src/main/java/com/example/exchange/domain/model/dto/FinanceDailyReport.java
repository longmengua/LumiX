/*
 * 檔案用途：財務日報 DTO，彙總 durable wallet ledger journal。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class FinanceDailyReport {

    private final LocalDate reportDate;

    private final Instant fromInclusive;

    private final Instant toExclusive;

    private final long entryCount;

    private final BigDecimal totalDebit;

    private final BigDecimal totalCredit;

    private final boolean balanced;

    private final Instant generatedAt;

    private final List<FinanceDailyReportLine> lines;
    public FinanceDailyReport(LocalDate reportDate, Instant fromInclusive, Instant toExclusive, long entryCount, BigDecimal totalDebit, BigDecimal totalCredit, boolean balanced, Instant generatedAt, List<FinanceDailyReportLine> lines) {
        totalDebit = totalDebit == null ? BigDecimal.ZERO : totalDebit;
        totalCredit = totalCredit == null ? BigDecimal.ZERO : totalCredit;
        lines = lines == null ? List.of() : List.copyOf(lines);
    
        this.reportDate = reportDate;
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
        this.entryCount = entryCount;
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
        this.balanced = balanced;
        this.generatedAt = generatedAt;
        this.lines = lines;
    }

    public LocalDate reportDate() {
        return reportDate;
    }

    public Instant fromInclusive() {
        return fromInclusive;
    }

    public Instant toExclusive() {
        return toExclusive;
    }

    public long entryCount() {
        return entryCount;
    }

    public BigDecimal totalDebit() {
        return totalDebit;
    }

    public BigDecimal totalCredit() {
        return totalCredit;
    }

    public boolean balanced() {
        return balanced;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<FinanceDailyReportLine> lines() {
        return lines;
    }
}