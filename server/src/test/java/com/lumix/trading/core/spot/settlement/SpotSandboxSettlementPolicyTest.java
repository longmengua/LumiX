package com.lumix.trading.core.spot.settlement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Spot sandbox settlement 只停在 design gate，不會被誤解成正式 settlement runtime。
 */
class SpotSandboxSettlementPolicyTest {

    private final SpotSandboxSettlementPolicy policy = new SpotSandboxSettlementPolicy();

    /**
     * 確認 settlement design contract 明確定義 step order、asset movement 語意與 idempotency / reconciliation 邊界。
     *
     * 這個 case 必須存在，因為 settlement 一旦沒有被設計 gate 鎖住，就很容易被誤接成正式資金流轉路徑。
     */
    @Test
    void describeReturnsExpectedSpotSandboxSettlementContract() {
        SpotSandboxSettlementDesign design = policy.describe();

        assertEquals(SpotSandboxSettlementDecision.DESIGN_ONLY, design.runtimeStatus());
        assertEquals(
                List.of(
                        SpotSandboxSettlementCapability.SETTLEMENT_INPUT_VALIDATION,
                        SpotSandboxSettlementCapability.RESERVATION_STATE_CHECK,
                        SpotSandboxSettlementCapability.BASE_ASSET_MOVEMENT,
                        SpotSandboxSettlementCapability.QUOTE_ASSET_MOVEMENT,
                        SpotSandboxSettlementCapability.RESERVATION_COMMIT_RELEASE_BOUNDARY,
                        SpotSandboxSettlementCapability.LEDGER_POSTING_GATE,
                        SpotSandboxSettlementCapability.BALANCE_PROJECTION_REFRESH_GATE,
                        SpotSandboxSettlementCapability.RECONCILIATION_CHECK,
                        SpotSandboxSettlementCapability.OUTBOX_AUDIT_BOUNDARY
                ),
                design.capabilities()
        );
        assertEquals(
                List.of(
                        SpotSandboxSettlementStep.IDEMPOTENCY_DECISION,
                        SpotSandboxSettlementStep.VALIDATE_SETTLEMENT_INPUT,
                        SpotSandboxSettlementStep.VERIFY_RESERVATION_STATE,
                        SpotSandboxSettlementStep.COMPUTE_ASSET_MOVEMENTS,
                        SpotSandboxSettlementStep.RESERVATION_COMMIT_RELEASE_DECISION,
                        SpotSandboxSettlementStep.LEDGER_POSTING_CONTROLLED_GATE,
                        SpotSandboxSettlementStep.BALANCE_PROJECTION_REFRESH_GATE,
                        SpotSandboxSettlementStep.RECONCILIATION_CHECK,
                        SpotSandboxSettlementStep.OUTBOX_AUDIT_BOUNDARY
                ),
                design.settlementSteps()
        );
        assertTrue(design.settlementRules().stream().anyMatch(text -> text.contains("settlement 是 explicit sandbox process")));
        assertTrue(design.settlementRules().stream().anyMatch(text -> text.contains("settlement input 只能來自 P16-T06 的 sandbox trade/fill result boundary")));
        assertTrue(design.settlementRules().stream().anyMatch(text -> text.contains("trade/fill result 在 settlement runtime 未完成前，只能停在 SETTLEMENT_NOT_STARTED")));
        assertTrue(design.settlementRules().stream().anyMatch(text -> text.contains("BUY side movement：buyer receives base asset，buyer pays quote asset")));
        assertTrue(design.settlementRules().stream().anyMatch(text -> text.contains("SELL side movement：seller pays base asset，seller receives quote asset")));
        assertTrue(design.settlementRules().stream().anyMatch(text -> text.contains("quoteAmount = price * quantity")));
        assertTrue(design.settlementRules().stream().anyMatch(text -> text.contains("amount / price / quantity / quoteAmount 一律使用 BigDecimal")));
        assertTrue(design.settlementRules().stream().anyMatch(text -> text.contains("settlement result 不等於 ledger posted / balance updated / reservation committed")));
        assertTrue(design.settlementRules().stream().anyMatch(text -> text.contains("settlement mismatch 必須進 reconciliation / human review")));
        assertTrue(design.settlementRules().stream().anyMatch(text -> text.contains("requestId 不是 idempotency guarantee")));
        assertTrue(design.settlementRules().stream().anyMatch(text -> text.contains("idempotencyKey 才能防 duplicate settlement")));
        assertTrue(design.settlementRules().stream().anyMatch(text -> text.contains("所有 settlement runtime、reservation runtime、ledger posting integration、balance refresh、reconciliation runtime 都屬於 HUMAN_REVIEW_REQUIRED")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增 settlement runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不更新 order book state")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不更新 trade/fill state")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增 DB write")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不接 reservation runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不接 ledger posting runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不接 balance projection rebuild runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不接 idempotency_keys / outbox_events / audit_logs runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不宣稱 production-ready")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不宣稱 settlement completed")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不宣稱 ledger posted")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不宣稱 balance updated")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不宣稱 reservation committed")));
    }

    /**
     * 確認 settlement policy 只回傳設計意圖，不會把 sandbox settlement 誤包成正式 runtime。
     *
     * 這個 case 必須存在，因為 settlement 是最容易被誤接成正式資金流轉路徑的高風險 boundary。
     */
    @Test
    void policyMethodsStateDesignIntentWithoutRuntime() {
        assertTrue(policy.requiresExplicitSandboxSettlementProcess());
        assertTrue(policy.requiresSettlementInputFromTradeFillBoundary());
        assertTrue(policy.requiresSettlementStepOrder());
        assertTrue(policy.requiresBuyerReceivesBaseAndPaysQuote());
        assertTrue(policy.requiresSellerPaysBaseAndReceivesQuote());
        assertTrue(policy.requiresQuoteAmountUsesPriceTimesQuantity());
        assertTrue(policy.forbidsRuntimeCompletionClaims());
        assertTrue(policy.requiresRequestIdNotIdempotencyGuarantee());
        assertTrue(policy.requiresIdempotencyKeyForDuplicateSettlementPrevention());
        assertTrue(policy.requiresHumanReviewForMismatch());
    }
}
