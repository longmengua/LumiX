package com.lumix.trading.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 驗證 Trading Runtime Core 只輸出 scope gate 與 safety contracts，不會變成 runtime implementation。
 */
class TradingRuntimeCoreSafetyPolicyTest {

    private final TradingRuntimeCoreSafetyPolicy policy = new TradingRuntimeCoreSafetyPolicy();

    /**
     * 確認 Trading Runtime Core 的 scope 與安全契約有明確定義。
     *
     * 這個 case 必須存在，因為 P15-T01 的重點就是先把 ledger、balance、reservation 與 settlement 的邊界說清楚。
     */
    @Test
    void describeReturnsExpectedScopeAndSafetyContracts() {
        TradingRuntimeCoreSafetyContract contract = policy.describe();

        assertEquals(4, contract.scopes().size());
        assertTrue(contract.scopes().contains(TradingRuntimeCoreScope.LEDGER_POSTING_INTEGRATION_GATE));
        assertTrue(contract.scopes().contains(TradingRuntimeCoreScope.BALANCE_PROJECTION_REBUILD_DESIGN));
        assertTrue(contract.scopes().contains(TradingRuntimeCoreScope.RESERVATION_HOLD_RELEASE_DESIGN));
        assertTrue(contract.scopes().contains(TradingRuntimeCoreScope.BASIC_RECONCILIATION_DESIGN));
        assertTrue(contract.scopeBoundaries().stream().anyMatch(text -> text.contains("balance_projections")));
        assertTrue(contract.safetyContracts().stream().anyMatch(text -> text.contains("BigDecimal")));
        assertTrue(contract.safetyContracts().stream().anyMatch(text -> text.contains("requestId 不等於 idempotency guarantee")));
        assertTrue(contract.noGoConditions().contains("真正過帳"));
        assertFalse(contract.earliestAllowedWork().isEmpty());
    }

    /**
     * 確認 safety contract 沒有把正式 runtime 誤包成已完成狀態。
     *
     * 這個 case 必須存在，因為任何 production-ready 暗示都會把後續交易風險提早放大。
     */
    @Test
    void safetyContractDoesNotClaimProductionReadiness() {
        TradingRuntimeCoreSafetyContract contract = policy.describe();

        assertFalse(contract.safetyContracts().stream().anyMatch(text -> text.contains("production-ready")));
        assertFalse(contract.scopeBoundaries().stream().anyMatch(text -> text.contains("正式交易上線")));
    }
}
