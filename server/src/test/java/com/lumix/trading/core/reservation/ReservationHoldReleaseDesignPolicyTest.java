package com.lumix.trading.core.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 reservation hold/release 只停在 design gate，不會被誤當成正式 runtime。
 */
class ReservationHoldReleaseDesignPolicyTest {

    private final ReservationHoldReleaseDesignPolicy policy = new ReservationHoldReleaseDesignPolicy();

    /**
     * 確認 reservation design contract 明確定義 hold / release / commit / cancel 的邊界。
     *
     * 這個 case 必須存在，因為 reservation 直接影響 available / locked balance，不能讓語意含糊到後續 runtime。
     */
    @Test
    void describeReturnsExpectedReservationDesignContract() {
        ReservationHoldReleaseDesign design = policy.describe();

        assertEquals(ReservationLifecycleDecision.DESIGN_ONLY, design.lifecycleDecision());
        assertEquals(
                List.of(
                        ReservationOperationType.HOLD,
                        ReservationOperationType.RELEASE,
                        ReservationOperationType.COMMIT,
                        ReservationOperationType.CANCEL
                ),
                design.operations()
        );
        assertTrue(design.lifecycleRules().stream().anyMatch(text -> text.contains("hold 會降低 available_amount，並增加 locked_amount")));
        assertTrue(design.lifecycleRules().stream().anyMatch(text -> text.contains("release 會增加 available_amount，並降低 locked_amount")));
        assertTrue(design.lifecycleRules().stream().anyMatch(text -> text.contains("commit 代表 reserved amount 被消耗，必須進入 settlement / ledger posting gate")));
        assertTrue(design.boundaryRules().stream().anyMatch(text -> text.contains("reservation 只能透過 application boundary 建立 / 釋放 / commit / cancel")));
        assertTrue(design.boundaryRules().stream().anyMatch(text -> text.contains("ledger 是 source of truth")));
        assertTrue(design.boundaryRules().stream().anyMatch(text -> text.contains("balance_projections 是 read model")));
        assertTrue(design.idempotencyRules().stream().anyMatch(text -> text.contains("requestId 不是 idempotency guarantee")));
        assertTrue(design.idempotencyRules().stream().anyMatch(text -> text.contains("idempotency key 才能防 duplicate hold / release")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("所有 reservation runtime 都屬於 HUMAN_REVIEW_REQUIRED")));
    }

    /**
     * 確認 policy 只回傳設計意圖，不會把 reservation 誤包成可直接執行的 runtime。
     *
     * 這個 case 必須存在，因為高風險 money movement 的設計 gate 不能偷渡成實作完成。
     */
    @Test
    void policyMethodsStateDesignIntentWithoutRuntime() {
        assertTrue(policy.definesHoldReleaseAffectsAvailableAndLocked());
        assertTrue(policy.requiresApplicationBoundary());
        assertTrue(policy.forbidsDirectLedgerMutation());
        assertTrue(policy.requiresIdempotencyForRuntime());
        assertTrue(policy.forbidsMatchingDirectWrites());
    }
}
