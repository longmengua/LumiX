package com.lumix.trading.core.futures.sandbox.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 驗證 P20-T04 review 只提供人類審查資訊，不會轉換成管理授權或自動修正。 */
class FuturesSandboxContractTradingAdminAuditReviewGateTest {
    private final FuturesSandboxContractTradingAdminAuditReviewGate reviewGate =
            new FuturesSandboxContractTradingAdminAuditReviewGate();

    @Test
    void requiresHumanReviewEvenWhenSandboxFlowReconciliationIsConsistent() {
        FuturesSandboxContractTradingReconciliationResult reconciliation =
                FuturesSandboxContractTradingReconciliationResult.consistent(
                        FuturesSandboxContractTradingFlowResult.eligible()
                );

        FuturesSandboxContractTradingAdminAuditReview review = reviewGate.review(reconciliation);

        assertEquals(FuturesSandboxContractTradingAdminAuditReviewDecision.HUMAN_REVIEW_REQUIRED, review.decision());
        assertEquals(
                FuturesSandboxContractTradingAdminAuditReviewReason.SANDBOX_FLOW_RECONCILIATION_CONSISTENT,
                review.reason()
        );
        assertEquals(reconciliation, review.reconciliation());
        assertTrue(review.requiredReviewActions().stream()
                .anyMatch(action -> action.contains("sandbox eligibility")));
        assertTrue(review.prohibitedActions().contains("不得執行 admin manual balance adjustment"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> review.prohibitedActions().add("不得修改 immutable review snapshot")
        );
    }

    @Test
    void escalatesMismatchToHumanInvestigationWithoutChangingRecordedFlow() {
        FuturesSandboxContractTradingFlowResult replayed = FuturesSandboxContractTradingFlowResult.rejected(
                FuturesSandboxContractTradingFlowReason.LIQUIDATION_SIMULATED
        );
        FuturesSandboxContractTradingFlowResult recorded = FuturesSandboxContractTradingFlowResult.eligible();
        FuturesSandboxContractTradingReconciliationResult reconciliation =
                FuturesSandboxContractTradingReconciliationResult.mismatch(replayed, recorded);

        FuturesSandboxContractTradingAdminAuditReview review = reviewGate.review(reconciliation);

        assertEquals(
                FuturesSandboxContractTradingAdminAuditReviewDecision.MISMATCH_REQUIRES_HUMAN_INVESTIGATION,
                review.decision()
        );
        assertEquals(
                FuturesSandboxContractTradingAdminAuditReviewReason.SANDBOX_FLOW_RECONCILIATION_MISMATCH,
                review.reason()
        );
        assertEquals(replayed, review.reconciliation().replayedFlow());
        assertEquals(recorded, review.reconciliation().recordedFlow());
        assertTrue(review.prohibitedActions().contains("不得覆寫 recorded flow 或自動修正 reconciliation mismatch"));
    }
}
