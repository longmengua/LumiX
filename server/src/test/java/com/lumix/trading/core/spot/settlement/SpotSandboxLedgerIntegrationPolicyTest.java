package com.lumix.trading.core.spot.settlement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Spot sandbox settlement 與 ledger integration 只停在 design gate，不會被誤解成正式 ledger runtime。
 */
class SpotSandboxLedgerIntegrationPolicyTest {

    private final SpotSandboxLedgerIntegrationPolicy policy = new SpotSandboxLedgerIntegrationPolicy();

    /**
     * 確認 integration design contract 明確定義 step order、risk 與 no-go 邊界。
     *
     * 這個 case 必須存在，因為 settlement plan 一旦接上 ledger runtime，風險會直接提升到資金安全等級。
     */
    @Test
    void describeReturnsExpectedSpotSandboxLedgerIntegrationContract() {
        SpotSandboxLedgerIntegrationDesign design = policy.describe();

        assertEquals(SpotSandboxLedgerIntegrationDecision.DESIGN_ONLY, design.runtimeStatus());
        assertEquals(
                List.of(
                        SpotSandboxLedgerIntegrationStep.IDEMPOTENCY_DECISION,
                        SpotSandboxLedgerIntegrationStep.RESERVATION_STATE_VERIFICATION,
                        SpotSandboxLedgerIntegrationStep.SETTLEMENT_INPUT_VALIDATION,
                        SpotSandboxLedgerIntegrationStep.LEDGER_CANDIDATE_INVARIANT_CHECK,
                        SpotSandboxLedgerIntegrationStep.HUMAN_REVIEW_GATE,
                        SpotSandboxLedgerIntegrationStep.LEDGER_POSTING_CONTROLLED_GATE,
                        SpotSandboxLedgerIntegrationStep.BALANCE_PROJECTION_REFRESH_GATE,
                        SpotSandboxLedgerIntegrationStep.RECONCILIATION_CHECK,
                        SpotSandboxLedgerIntegrationStep.OUTBOX_AUDIT_BOUNDARY
                ),
                design.integrationSteps()
        );
        assertEquals(
                List.of(
                        SpotSandboxLedgerIntegrationRisk.DUPLICATE_SETTLEMENT,
                        SpotSandboxLedgerIntegrationRisk.LEDGER_APPEND_FAILURE,
                        SpotSandboxLedgerIntegrationRisk.BALANCE_REFRESH_LAG,
                        SpotSandboxLedgerIntegrationRisk.RESERVATION_COMMIT_MISMATCH,
                        SpotSandboxLedgerIntegrationRisk.RECONCILIATION_MISMATCH
                ),
                design.risks()
        );
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("settlement plan ledger candidate 只是 candidate，不是 posted journal")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("接 LedgerPostingRuntimeGate 前必須完成 idempotency decision")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("接 LedgerPostingRuntimeGate 前必須完成 reservation state verification")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("接 LedgerPostingRuntimeGate 前必須完成 settlement input validation")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("接 LedgerPostingRuntimeGate 前必須完成 ledger candidate invariant check")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("接 LedgerPostingRuntimeGate 前必須完成 human review gate")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("ledger posting controlled gate 成功後，才可以標示 ledger append completed")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("ledger append completed 不等於 balance projection refreshed")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("ledger append completed 不等於 reservation committed")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("ledger append completed 不等於 settlement fully completed")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("ledger append completed 不等於 reconciliation completed")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("duplicate settlement 必須由 idempotency key 控制，不得靠 requestId")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("requestId 只做 trace / correlation / audit linkage")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("四腿 ledger candidate 必須保留 buyer base CREDIT quantity / buyer quote DEBIT quoteAmount / seller base DEBIT quantity / seller quote CREDIT quoteAmount")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("outbox / audit 必須是後續 explicit boundary，不得偷寫")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增 ledger posting integration runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不呼叫 LedgerPostingRuntimeGate")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不呼叫 LedgerAppendOnlyJdbcAdapter")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不呼叫 BalanceProjectionRebuildGate")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不寫 ledger_journals / ledger_entries")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不更新 balance_projections")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不更新 reservations")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不接 idempotency_keys / outbox_events / audit_logs runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不宣稱 production-ready")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不宣稱 ledger posted")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不宣稱 settlement completed")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不宣稱 balance updated")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不宣稱 reservation committed")));
    }

    /**
     * 確認 policy 只回傳設計意圖，不會把 integration 誤包成正式 runtime。
     *
     * 這個 case 必須存在，因為接正式 ledger runtime 前必須先保留 design gate，而不是直接偷接。
     */
    @Test
    void policyMethodsStateDesignIntentWithoutRuntime() {
        assertTrue(policy.requiresDesignOnly());
        assertTrue(policy.requiresIdempotencyDecisionBeforeLedgerRuntime());
        assertTrue(policy.requiresReservationStateVerificationBeforeLedgerRuntime());
        assertTrue(policy.requiresSettlementInputValidationBeforeLedgerRuntime());
        assertTrue(policy.requiresLedgerCandidateInvariantCheckBeforeLedgerRuntime());
        assertTrue(policy.requiresHumanReviewGateBeforeLedgerRuntime());
        assertTrue(policy.forbidsCompletionClaimsBeyondLedgerAppend());
        assertTrue(policy.requiresRequestIdOnlyForTraceAndAuditLinkage());
        assertTrue(policy.requiresIdempotencyKeyForDuplicateSettlementControl());
        assertTrue(policy.requiresOutboxAuditAsExplicitBoundary());
    }
}
