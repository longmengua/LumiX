/*
 * 檔案用途：trial balance 結果，用於財務對帳與報表基礎。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trial balance report for a scoped ledger query.
 */
public record TrialBalanceReport(
        long uid,
        String asset,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean balanced,
        List<TrialBalanceLine> lines
) {
}
