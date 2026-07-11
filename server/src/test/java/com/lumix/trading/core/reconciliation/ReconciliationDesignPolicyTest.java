package com.lumix.trading.core.reconciliation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 reconciliation 只停在 design gate，不會被誤當成正式對帳 runtime。
 */
class ReconciliationDesignPolicyTest {

    private final ReconciliationDesignPolicy policy = new ReconciliationDesignPolicy();

    /**
     * 確認 reconciliation design contract 明確定義 ledger / projection / reservation / settlement 的對帳語意。
     *
     * 這個 case 必須存在，因為對帳一旦混淆 source of truth 與 read model，就會把 mismatch 修正成資金風險。
     */
    @Test
    void describeReturnsExpectedReconciliationDesignContract() {
        ReconciliationDesign design = policy.describe();

        assertEquals(ReconciliationDesignDecision.DESIGN_ONLY, design.decision());
        assertEquals(
                List.of(
                        ReconciliationSignalType.LEDGER_BALANCE_MISMATCH,
                        ReconciliationSignalType.RESERVATION_LOCK_MISMATCH,
                        ReconciliationSignalType.SETTLEMENT_EXPECTATION_MISMATCH,
                        ReconciliationSignalType.PROJECTION_LAG_EXCEEDED
                ),
                design.signalTypes()
        );
        assertTrue(design.comparisonRules().stream().anyMatch(text -> text.contains("ledger 是 source of truth")));
        assertTrue(design.comparisonRules().stream().anyMatch(text -> text.contains("balance_projections 是 read model")));
        assertTrue(design.comparisonRules().stream().anyMatch(text -> text.contains("reservation 是 hold/release 狀態模型，不是 ledger entry 替代品")));
        assertTrue(design.comparisonRules().stream().anyMatch(text -> text.contains("settlement 是 explicit process")));
        assertTrue(design.mismatchRules().stream().anyMatch(text -> text.contains("ledger_entries derived totals")));
        assertTrue(design.mismatchRules().stream().anyMatch(text -> text.contains("mismatch 必須產生 review / incident / repair flow")));
        assertTrue(design.mismatchRules().stream().anyMatch(text -> text.contains("requestId 不是 idempotency guarantee")));
        assertTrue(design.mismatchRules().stream().anyMatch(text -> text.contains("audit / outbox / idempotency 邊界")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("所有 reconciliation runtime 都屬於 HUMAN_REVIEW_REQUIRED")));
    }

    /**
     * 確認 policy helper 只回傳設計意圖，不會把對帳誤包成 runtime 完成。
     *
     * 這個 case 必須存在，因為 reconciliation 設計 gate 不能偷偷變成自動修正引擎。
     */
    @Test
    void policyMethodsStateDesignIntentWithoutRuntime() {
        assertTrue(policy.requiresLedgerAsSourceOfTruth());
        assertTrue(policy.requiresBalanceProjectionAsReadModel());
        assertTrue(policy.requiresReservationLockedAmountCheck());
        assertTrue(policy.requiresSettlementExpectationCheck());
        assertTrue(policy.forbidsSilentAutoRepair());
        assertTrue(policy.requiresHumanReviewForMismatch());
    }
}
