package com.lumix.trading.core.spot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Spot Trading Sandbox 只停在 scope gate / runtime boundary，不會被誤解為正式交易 runtime。
 */
class SpotSandboxBoundaryPolicyTest {

    private final SpotSandboxBoundaryPolicy policy = new SpotSandboxBoundaryPolicy();

    /**
     * 確認 spot sandbox contract 明確定義 sandbox-only 範圍與 runtime boundaries。
     *
     * 這個 case 必須存在，因為 spot sandbox 只能在受控邊界內逐步接線，不能偷渡成 production trading。
     */
    @Test
    void describeReturnsExpectedSpotSandboxBoundaryContract() {
        SpotSandboxBoundaryDesign design = policy.describe();

        assertEquals(SpotSandboxRuntimeStatus.SANDBOX_BOUNDARY_DEFINED, design.runtimeStatus());
        assertEquals(
                List.of(
                        SpotSandboxCapability.SCOPE_GATE,
                        SpotSandboxCapability.ORDER_INTAKE_BOUNDARY,
                        SpotSandboxCapability.RESERVATION_BOUNDARY,
                        SpotSandboxCapability.MATCHING_BOUNDARY,
                        SpotSandboxCapability.SETTLEMENT_BOUNDARY,
                        SpotSandboxCapability.LEDGER_POSTING_GATE,
                        SpotSandboxCapability.BALANCE_PROJECTION_REBUILD_GATE,
                        SpotSandboxCapability.RECONCILIATION_BOUNDARY
                ),
                design.capabilities()
        );
        assertTrue(design.sandboxRules().stream().anyMatch(text -> text.contains("sandbox only")));
        assertTrue(design.sandboxRules().stream().anyMatch(text -> text.contains("not production-ready")));
        assertTrue(design.sandboxRules().stream().anyMatch(text -> text.contains("not public user trading ready")));
        assertTrue(design.sandboxRules().stream().anyMatch(text -> text.contains("no real money")));
        assertTrue(design.sandboxRules().stream().anyMatch(text -> text.contains("no external market connectivity")));
        assertTrue(design.boundaryRules().stream().anyMatch(text -> text.contains("sandbox order intake")));
        assertTrue(design.boundaryRules().stream().anyMatch(text -> text.contains("sandbox reservation hold/release")));
        assertTrue(design.boundaryRules().stream().anyMatch(text -> text.contains("sandbox matching")));
        assertTrue(design.boundaryRules().stream().anyMatch(text -> text.contains("sandbox settlement")));
        assertTrue(design.boundaryRules().stream().anyMatch(text -> text.contains("ledger posting controlled gate")));
        assertTrue(design.boundaryRules().stream().anyMatch(text -> text.contains("balance projection rebuild gate")));
        assertTrue(design.boundaryRules().stream().anyMatch(text -> text.contains("reconciliation boundary")));
        assertTrue(design.boundaryRules().stream().anyMatch(text -> text.contains("requestId 不是 idempotency guarantee")));
        assertTrue(design.boundaryRules().stream().anyMatch(text -> text.contains("idempotency key 才是 duplicate prevention contract")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增正式 order placement runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增 matching runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增 settlement runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增 reservation runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增外部市場連線")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增真實資金流")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增 withdrawal runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增 futures / margin / liquidation runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("所有 money movement / settlement / reservation runtime 都屬於 HUMAN_REVIEW_REQUIRED")));
    }

    /**
     * 確認 policy 只回傳設計意圖，不會把 spot sandbox 誤包成正式 runtime。
     *
     * 這個 case 必須存在，因為 sandbox boundary 一旦被誤解成 production-ready，就會直接繞過 Phase 15 邊界。
     */
    @Test
    void policyMethodsStateDesignIntentWithoutRuntime() {
        assertTrue(policy.requiresSandboxOnly());
        assertTrue(policy.forbidsProductionReadyClaim());
        assertTrue(policy.requiresOrderIntakeBoundary());
        assertTrue(policy.requiresReservationBoundary());
        assertTrue(policy.requiresMatchingBoundary());
        assertTrue(policy.requiresSettlementBoundary());
        assertTrue(policy.requiresLedgerPostingGate());
        assertTrue(policy.requiresBalanceProjectionRebuildGate());
        assertTrue(policy.requiresReconciliationBoundary());
        assertTrue(policy.forbidsDirectLedgerWriteFromOrderOrMatching());
        assertTrue(policy.forbidsDirectBalanceProjectionWriteFromOrderOrMatching());
        assertTrue(policy.requiresBigDecimalForTradingAmounts());
    }
}
