package com.lumix.application.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 驗證 transaction boundary policy 不會把高風險流程誤判成 read-only。
 */
class TransactionBoundaryPolicyTest {

    private final TransactionBoundaryPolicy policy = new TransactionBoundaryPolicy();

    /**
     * 確認純查詢會落在 read-only boundary。
     */
    @Test
    void readOnlyQueryUsesReadOnlyBoundary() {
        assertEquals(TransactionBoundaryKind.READ_ONLY, policy.classify(TransactionUseCase.READ_ONLY_QUERY));
        assertFalse(policy.requiresHumanReview(TransactionUseCase.READ_ONLY_QUERY));
    }

    /**
     * 確認 ledger posting 一定是 write 並保留 HUMAN_REVIEW_REQUIRED。
     *
     * 這個 case 必須存在，因為 ledger 屬於高風險流程，不能被當成普通查詢。
     */
    @Test
    void ledgerPostingRequiresWriteBoundaryAndHumanReview() {
        assertEquals(TransactionBoundaryKind.WRITE, policy.classify(TransactionUseCase.LEDGER_POSTING));
        assertTrue(policy.requiresHumanReview(TransactionUseCase.LEDGER_POSTING));
    }

    /**
     * 確認 reservation hold / release 也不能被誤判為 read-only。
     */
    @Test
    void reservationHoldReleaseRequiresWriteBoundaryAndHumanReview() {
        assertEquals(TransactionBoundaryKind.WRITE, policy.classify(TransactionUseCase.RESERVATION_HOLD_RELEASE));
        assertTrue(policy.requiresHumanReview(TransactionUseCase.RESERVATION_HOLD_RELEASE));
    }

    /**
     * 確認 outbox / idempotency / audit 屬於 write，但不應被誤標成資金類高風險流程。
     */
    @Test
    void outboxIdempotencyAuditStayWriteWithoutHumanReview() {
        assertEquals(TransactionBoundaryKind.WRITE, policy.classify(TransactionUseCase.OUTBOX_APPEND));
        assertEquals(TransactionBoundaryKind.WRITE, policy.classify(TransactionUseCase.IDEMPOTENCY_RECORD));
        assertEquals(TransactionBoundaryKind.WRITE, policy.classify(TransactionUseCase.AUDIT_APPEND));
        assertFalse(policy.requiresHumanReview(TransactionUseCase.OUTBOX_APPEND));
        assertFalse(policy.requiresHumanReview(TransactionUseCase.IDEMPOTENCY_RECORD));
        assertFalse(policy.requiresHumanReview(TransactionUseCase.AUDIT_APPEND));
    }
}
