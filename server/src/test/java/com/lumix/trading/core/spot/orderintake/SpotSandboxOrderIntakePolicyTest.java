package com.lumix.trading.core.spot.orderintake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Spot sandbox order intake 只停在 design gate，不會被誤解為正式 order placement runtime。
 */
class SpotSandboxOrderIntakePolicyTest {

    private final SpotSandboxOrderIntakePolicy policy = new SpotSandboxOrderIntakePolicy();

    /**
     * 確認 intake design contract 明確描述 sandbox-only 與 validation 邊界。
     *
     * 這個 case 必須存在，因為 order intake 一旦沒鎖好，就會直接把 sandbox command 誤送進後續 runtime。
     */
    @Test
    void describeReturnsExpectedSpotSandboxOrderIntakeContract() {
        SpotSandboxOrderIntakeDesign design = policy.describe();

        assertTrue(policy.requiresSandboxOnly());
        assertTrue(policy.forbidsProductionReadyClaim());
        assertTrue(policy.acceptsOnlyLimitOrders());
        assertTrue(policy.acceptsOnlyGtc());
        assertTrue(policy.requiresBigDecimalForTradingAmounts());
        assertTrue(policy.forbidsDirectPersistence());
        assertTrue(policy.forbidsBoundaryBypass());

        assertEquals(
                List.of(
                        "sandbox order intake boundary",
                        "in-memory validation only",
                        "requestId 只做 trace / correlation",
                        "idempotencyKey 必須存在，但本題不做 idempotency store / lookup",
                        "accepted result 不代表 persisted / reserved / matched / settled / posted",
                        "amount / price / quantity 一律使用 BigDecimal，不得使用 float / double"
                ),
                design.capabilities()
        );
        assertTrue(design.intakeRules().stream().anyMatch(text -> text.contains("sandbox only")));
        assertTrue(design.intakeRules().stream().anyMatch(text -> text.contains("not production-ready")));
        assertTrue(design.intakeRules().stream().anyMatch(text -> text.contains("not public user trading ready")));
        assertTrue(design.intakeRules().stream().anyMatch(text -> text.contains("no real money")));
        assertTrue(design.validationRules().stream().anyMatch(text -> text.contains("只受理 LIMIT order")));
        assertTrue(design.validationRules().stream().anyMatch(text -> text.contains("只受理 GTC time-in-force")));
        assertTrue(design.validationRules().stream().anyMatch(text -> text.contains("price 必須大於 0")));
        assertTrue(design.validationRules().stream().anyMatch(text -> text.contains("quantity 必須大於 0")));
        assertTrue(design.validationRules().stream().anyMatch(text -> text.contains("marketSymbol 必須非空")));
        assertTrue(design.validationRules().stream().anyMatch(text -> text.contains("userId 與 accountId 必須非空")));
        assertTrue(design.rejectionReasons().contains(SpotSandboxOrderRejectionReason.MISSING_IDEMPOTENCY_KEY));
        assertTrue(design.rejectionReasons().contains(SpotSandboxOrderRejectionReason.UNSUPPORTED_ORDER_TYPE));
        assertTrue(design.rejectionReasons().contains(SpotSandboxOrderRejectionReason.UNSUPPORTED_TIME_IN_FORCE));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增正式 order placement runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不接 reservation / matching / settlement / ledger / balance projection runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("所有後續 reservation / matching / settlement 仍屬 HUMAN_REVIEW_REQUIRED")));
    }
}
