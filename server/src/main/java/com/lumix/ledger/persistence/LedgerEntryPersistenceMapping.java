package com.lumix.ledger.persistence;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.ledger.domain.LedgerDirection;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * ledger_entries 的欄位 mapping contract。
 *
 * 這份 mapping 只描述 append-only entry shape；ledger_journal_id 在這個階段保留為可解析參考，
 * 不代表已經執行任何資料庫寫入。
 */
public record LedgerEntryPersistenceMapping(
        Optional<Long> ledgerJournalId,
        long entrySequence,
        AccountId accountId,
        AssetSymbol assetSymbol,
        LedgerDirection direction,
        BigDecimal amount
) {

    public LedgerEntryPersistenceMapping {
        // ledger entry mapping 只能描述欄位，不允許在這裡偷偷做 runtime mutation。
        Objects.requireNonNull(ledgerJournalId, "ledgerJournalId must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(assetSymbol, "assetSymbol must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (entrySequence <= 0L) {
            throw new IllegalArgumentException("entrySequence must be greater than zero");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }
}
