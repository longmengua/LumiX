/*
 * 檔案用途：財務日報單列 DTO，依 reason / asset / account code 彙總 ledger postings。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

public record FinanceDailyReportLine(
        String reason,
        String asset,
        String accountCode,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal netDebit,
        BigDecimal netCredit
) {
    public FinanceDailyReportLine {
        debit = debit == null ? BigDecimal.ZERO : debit;
        credit = credit == null ? BigDecimal.ZERO : credit;
        BigDecimal net = debit.subtract(credit);
        netDebit = net.signum() > 0 ? net : BigDecimal.ZERO;
        netCredit = net.signum() < 0 ? net.abs() : BigDecimal.ZERO;
    }
}
