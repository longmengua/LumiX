package com.lumix.trading.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.trading.core.reservation.ReservationLifecycleDecision;
import com.lumix.trading.core.reservation.ReservationOperationType;
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
        assertTrue(contract.scopeBoundaries().stream().anyMatch(text -> text.contains("reservation hold 會降低 available_amount，release 會增加 available_amount")));
        assertTrue(contract.scopeBoundaries().stream().anyMatch(text -> text.contains("reservation commit 必須經 settlement / ledger posting gate")));
        assertTrue(contract.safetyContracts().stream().anyMatch(text -> text.contains("BigDecimal")));
        assertTrue(contract.safetyContracts().stream().anyMatch(text -> text.contains("requestId 不等於 idempotency guarantee")));
        assertTrue(contract.safetyContracts().stream().anyMatch(text -> text.contains("reservation hold / release 必須先做 idempotency decision，再做 hold / release")));
        assertTrue(contract.noGoConditions().contains("真正過帳"));
        assertTrue(contract.noGoConditions().contains("所有 reservation runtime 都屬於 HUMAN_REVIEW_REQUIRED"));
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

    /**
     * 確認 reservation 的設計契約已被 Trading Runtime Core 安全政策收進來。
     *
     * 這個 case 必須存在，因為 reservation hold / release 會直接影響 available / locked balance 語意。
     */
    @Test
    void reservationHoldReleaseDesignIsExplicit() {
        var design = policy.describeReservationHoldReleaseDesign();

        assertEquals(ReservationLifecycleDecision.DESIGN_ONLY, design.lifecycleDecision());
        assertTrue(design.operations().contains(ReservationOperationType.HOLD));
        assertTrue(design.operations().contains(ReservationOperationType.RELEASE));
        assertTrue(design.operations().contains(ReservationOperationType.COMMIT));
        assertTrue(design.operations().contains(ReservationOperationType.CANCEL));
        assertTrue(design.boundaryRules().stream().anyMatch(text -> text.contains("reservation 只能透過 application boundary 建立 / 釋放 / commit / cancel")));
        assertTrue(design.idempotencyRules().stream().anyMatch(text -> text.contains("idempotency key 才能防 duplicate hold / release")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("所有 reservation runtime 都屬於 HUMAN_REVIEW_REQUIRED")));
    }
}
