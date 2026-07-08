package com.lumix.ledger.application.posting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.DefaultLedgerRuntimeBoundary;
import com.lumix.ledger.application.LedgerRuntimeBoundary;
import com.lumix.ledger.application.LedgerRuntimePrerequisite;
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
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 驗證 ledger posting command boundary 只產生 plan，不會偷偷變成正式 posting runtime。
 */
class LedgerPostingCommandBoundaryTest {

    private final LedgerRuntimeBoundary runtimeBoundary = new DefaultLedgerRuntimeBoundary();
    private final LedgerInvariantPolicy invariantPolicy = new LedgerInvariantPolicy();
    private final LedgerPostingCommandBoundary boundary = new DefaultLedgerPostingCommandBoundary(
            runtimeBoundary,
            invariantPolicy,
            new DefaultLedgerAppendOnlyPersistenceMapper()
    );

    /**
     * 確認有效 command 只會產生 accepted plan，不代表已寫入任何資料。
     *
     * 這個 case 必須存在，因為 command boundary 的責任是產生可後續審核的 plan，而不是完成寫入。
     */
    @Test
    void validCommandProducesAcceptedPlanOnly() {
        LedgerPostingCommand command = new LedgerPostingCommand(
                new RequestId("req-10001"),
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ORDER,
                        "order-10001",
                        List.of(
                                new LedgerEntryDraft(new AccountId("acct-a"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                        new BigDecimal("25"), 1L),
                                new LedgerEntryDraft(new AccountId("acct-b"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                        new BigDecimal("25"), 2L)
                        )
                ),
                Instant.parse("2026-07-08T12:00:00Z")
        );

        LedgerPostingCommandResult result = boundary.evaluate(
                command,
                EnumSet.allOf(LedgerRuntimePrerequisite.class)
        );

        assertEquals(LedgerPostingDecision.ACCEPTED, result.decision());
        assertTrue(result.plan().isPresent());
        assertFalse(result.rejection().isPresent());
        assertEquals(command.requestId(), result.plan().orElseThrow().command().requestId());
    }

    /**
     * 確認 prerequisite gate 失敗時，command 會被拒絕。
     *
     * 這個 case 保護的是 gate 順序：runtime prerequisites 沒有到位時，不應再談 journal invariant。
     */
    @Test
    void prerequisiteFailureRejectsCommand() {
        RecordingInvariantPolicy recordingPolicy = new RecordingInvariantPolicy();
        LedgerPostingCommandBoundary gate = new DefaultLedgerPostingCommandBoundary(
                runtimeBoundary,
                recordingPolicy,
                new DefaultLedgerAppendOnlyPersistenceMapper()
        );

        LedgerPostingCommand command = new LedgerPostingCommand(
                new RequestId("req-10002"),
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ORDER,
                        "order-10002",
                        List.of(
                                new LedgerEntryDraft(new AccountId("acct-a"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                        new BigDecimal("10"), 1L),
                                new LedgerEntryDraft(new AccountId("acct-b"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                        new BigDecimal("10"), 2L)
                        )
                ),
                Instant.parse("2026-07-08T12:05:00Z")
        );

        LedgerPostingCommandResult result = gate.evaluate(command, EnumSet.noneOf(LedgerRuntimePrerequisite.class));

        assertEquals(LedgerPostingDecision.REJECTED, result.decision());
        assertTrue(result.rejection().isPresent());
        assertFalse(result.plan().isPresent());
        assertEquals("PREREQUISITE_GATE_FAILED", result.rejection().orElseThrow().reasonCode());
        assertFalse(recordingPolicy.wasValidateCalled());
    }

    /**
     * 確認 invariant policy 失敗時，command 會被拒絕。
     *
     * 這個 case 必須存在，因為 prerequisite 通過不代表 journal draft 已符合雙錄帳規則。
     */
    @Test
    void invariantFailureRejectsCommand() {
        LedgerPostingCommand command = new LedgerPostingCommand(
                new RequestId("req-10003"),
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.FEE,
                        "fee-10003",
                        List.of(
                                new LedgerEntryDraft(new AccountId("acct-a"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                        new BigDecimal("10"), 1L),
                                new LedgerEntryDraft(new AccountId("acct-b"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                        new BigDecimal("9"), 2L)
                        )
                ),
                Instant.parse("2026-07-08T12:10:00Z")
        );

        LedgerPostingCommandResult result = boundary.evaluate(
                command,
                EnumSet.allOf(LedgerRuntimePrerequisite.class)
        );

        assertEquals(LedgerPostingDecision.REJECTED, result.decision());
        assertTrue(result.rejection().isPresent());
        assertFalse(result.plan().isPresent());
        assertEquals("JOURNAL_INVARIANT_FAILED", result.rejection().orElseThrow().reasonCode());
        assertFalse(result.rejection().orElseThrow().safeDetails().isEmpty());
    }

    /**
     * 驗證 prerequisite gate 與 invariant gate 的順序。
     *
     * prerequisite 不成立時，invariant policy 不應被呼叫，避免把環境問題誤判成 journal 問題。
     */
    @Test
    void prerequisiteGateRunsBeforeInvariantPolicy() {
        RecordingInvariantPolicy recordingPolicy = new RecordingInvariantPolicy();
        LedgerPostingCommandBoundary gate = new DefaultLedgerPostingCommandBoundary(
                runtimeBoundary,
                recordingPolicy,
                new DefaultLedgerAppendOnlyPersistenceMapper()
        );

        LedgerPostingCommand command = new LedgerPostingCommand(
                new RequestId("req-10004"),
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.TRADE,
                        "trade-10004",
                        List.of(
                                new LedgerEntryDraft(new AccountId("acct-a"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                        new BigDecimal("5"), 1L),
                                new LedgerEntryDraft(new AccountId("acct-b"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                        new BigDecimal("5"), 2L)
                        )
                ),
                Instant.parse("2026-07-08T12:15:00Z")
        );

        LedgerPostingCommandResult result = gate.evaluate(command, EnumSet.noneOf(LedgerRuntimePrerequisite.class));

        assertEquals(LedgerPostingDecision.REJECTED, result.decision());
        assertFalse(recordingPolicy.wasValidateCalled());
    }

    /**
     * 只用來確認 boundary 不會在 prerequisite 不成立時偷偷進入 invariant 檢查。
     */
    private static final class RecordingInvariantPolicy extends LedgerInvariantPolicy {
        private boolean validateCalled;

        @Override
        public List<com.lumix.ledger.domain.LedgerInvariantViolation> validate(LedgerJournalDraft journalDraft) {
            validateCalled = true;
            return super.validate(journalDraft);
        }

        /**
         * 回報 validate 是否曾被呼叫。
         */
        boolean wasValidateCalled() {
            return validateCalled;
        }
    }
}
