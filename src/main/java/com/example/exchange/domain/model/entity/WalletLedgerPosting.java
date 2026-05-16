package com.example.exchange.domain.model.entity;

import java.math.BigDecimal;

/**
 * 雙式簿記的一條分錄。
 */
public record WalletLedgerPosting(
        String accountCode,
        String asset,
        BigDecimal debit,
        BigDecimal credit
) {
    public WalletLedgerPosting {
        debit = debit == null ? BigDecimal.ZERO : debit;
        credit = credit == null ? BigDecimal.ZERO : credit;
    }
}
