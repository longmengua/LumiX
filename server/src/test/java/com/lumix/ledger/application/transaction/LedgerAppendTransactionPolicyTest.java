package com.lumix.ledger.application.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.DefaultLedgerRuntimeBoundary;
import com.lumix.ledger.application.LedgerRuntimeBoundary;
import com.lumix.ledger.application.LedgerRuntimePrerequisite;
import com.lumix.ledger.application.posting.DefaultLedgerPostingCommandBoundary;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.application.posting.LedgerPostingCommandBoundary;
import com.lumix.ledger.application.posting.LedgerPostingCommandResult;
import com.lumix.ledger.application.posting.LedgerPostingDecision;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerInvariantPolicy;
import com.lumix.ledger.domain.LedgerJournalDraft;
import com.lumix.ledger.persistence.DefaultLedgerAppendOnlyPersistenceMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 ledger append transaction 目前只輸出設計，不做任何 DB 寫入。
 */
class LedgerAppendTransactionPolicyTest {

    private final LedgerRuntimeBoundary runtimeBoundary = new DefaultLedgerRuntimeBoundary();
    private final LedgerPostingCommandBoundary postingBoundary = new DefaultLedgerPostingCommandBoundary(
            runtimeBoundary,
            new LedgerInvariantPolicy(),
            new DefaultLedgerAppendOnlyPersistenceMapper()
    );
    private final LedgerAppendTransactionBoundary transactionBoundary = new LedgerAppendTransactionPolicy();

    /**
     * 確認 transaction design 包含單一 transaction 內預期的步驟。
     *
     * 這個 case 必須存在，因為未來正式 append runtime 需要明確知道 idempotency / journal / outbox / audit 的順序。
     */
    @Test
    void transactionDesignContainsSingleTransactionSteps() {
        LedgerPostingCommandResult accepted = postingBoundary.evaluate(
                new LedgerPostingCommand(
                        new RequestId("req-20001"),
                        new LedgerJournalDraft(
                                LedgerBusinessReferenceType.ORDER,
                                "order-20001",
                                List.of(
                                        new LedgerEntryDraft(new AccountId("acct-a"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                                new BigDecimal("8"), 1L),
                                        new LedgerEntryDraft(new AccountId("acct-b"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                                new BigDecimal("8"), 2L)
                                )
                        ),
                        Instant.parse("2026-07-08T13:00:00Z")
                ),
                EnumSet.allOf(LedgerRuntimePrerequisite.class)
        );

        LedgerAppendTransactionDesign design = transactionBoundary.describe(accepted.plan().orElseThrow());

        assertEquals(new RequestId("req-20001"), design.requestId());
        assertTrue(design.steps().contains(LedgerAppendTransactionStep.IDEMPOTENCY_CHECK_OR_LOCK));
        assertTrue(design.steps().contains(LedgerAppendTransactionStep.JOURNAL_HEADER_APPEND));
        assertTrue(design.steps().contains(LedgerAppendTransactionStep.JOURNAL_ENTRIES_APPEND));
        assertTrue(design.steps().contains(LedgerAppendTransactionStep.OUTBOX_APPEND));
        assertTrue(design.steps().contains(LedgerAppendTransactionStep.AUDIT_APPEND));
        assertTrue(design.steps().contains(LedgerAppendTransactionStep.COMMIT));
        assertFalse(design.safetyNotes().isEmpty());
    }
}
