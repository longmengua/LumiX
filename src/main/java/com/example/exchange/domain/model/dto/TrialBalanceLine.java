/*
 * 檔案用途：trial balance 單一 account code 的借貸合計。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

/**
 * Trial balance line by asset/account code.
 */
public record TrialBalanceLine(
        String asset,
        String accountCode,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal netDebit,
        BigDecimal netCredit
) {
}
