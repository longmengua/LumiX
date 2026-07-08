package com.lumix.ledger.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerJournalDraft;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 ledger persistence contract 只描述 mapping，不會偷偷變成 DB write 實作。
 */
class LedgerAppendOnlyPersistenceMappingTest {

    private final LedgerAppendOnlyPersistencePort mapper = new DefaultLedgerAppendOnlyPersistenceMapper();

    /**
     * 確認 mapping contract 對齊 Phase 12 的 ledger_journals 與 ledger_entries 必要欄位。
     *
     * 這個 case 必須存在，因為 persistence contract 的首要責任是欄位對應清楚，而不是寫入。
     */
    @Test
    void mappingAlignsWithPhaseTwelveSchemaFields() {
        LedgerJournalDraft draft = new LedgerJournalDraft(
                LedgerBusinessReferenceType.DEPOSIT,
                "deposit-9001",
                List.of(
                        new LedgerEntryDraft(new AccountId("acct-1"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                new BigDecimal("10"), 1L),
                        new LedgerEntryDraft(new AccountId("acct-2"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                new BigDecimal("10"), 2L)
                )
        );

        LedgerAppendOnlyPersistenceMapping mapping = mapper.describeAppendOnlyMapping(
                draft,
                Instant.parse("2026-07-08T00:00:00Z")
        );

        assertEquals(LedgerBusinessReferenceType.DEPOSIT, mapping.journal().businessReferenceType());
        assertEquals("deposit-9001", mapping.journal().businessReferenceId());
        assertNull(mapping.journal().requestId());
        assertNull(mapping.journal().journalNote());
        assertEquals(Instant.parse("2026-07-08T00:00:00Z"), mapping.journal().postedAt());
        assertEquals(2, mapping.entries().size());
        assertTrue(mapping.entries().stream().allMatch(entry -> entry.ledgerJournalId().isEmpty()));
        assertEquals(1L, mapping.entries().get(0).entrySequence());
        assertEquals("acct-1", mapping.entries().get(0).accountId().value());
        assertEquals("USDT", mapping.entries().get(0).assetSymbol().value());
    }
}
