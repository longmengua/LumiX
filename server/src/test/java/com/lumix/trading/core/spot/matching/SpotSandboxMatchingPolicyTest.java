package com.lumix.trading.core.spot.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Spot sandbox matching 只停在 design gate，不會被誤解為正式 matching engine。
 */
class SpotSandboxMatchingPolicyTest {

    private final SpotSandboxMatchingPolicy policy = new SpotSandboxMatchingPolicy();

    /**
     * 確認 matching design contract 明確定義 market partition、price/time priority 與 settlement input boundary。
     *
     * 這個 case 必須存在，因為 matching 一旦沒鎖住，就很容易被誤接成正式撮合 runtime。
     */
    @Test
    void describeReturnsExpectedSpotSandboxMatchingContract() {
        SpotSandboxMatchingDesign design = policy.describe();

        assertTrue(policy.requiresExplicitSandboxMatchingProcess());
        assertTrue(policy.requiresMarketPartition());
        assertTrue(policy.requiresCrossedLimitPrice());
        assertTrue(policy.requiresBuyHigherPricePriority());
        assertTrue(policy.requiresSellLowerPricePriority());
        assertTrue(policy.requiresTimePriority());
        assertTrue(policy.requiresPartialFillSemantics());
        assertTrue(policy.requiresSettlementInputBoundary());
        assertTrue(policy.requiresHumanReviewForTradePriceRule());
        assertTrue(policy.forbidsRuntimeCompletionClaims());
        assertTrue(policy.forbidsTradePersistenceRuntime());
        assertTrue(policy.forbidsOrderBookStateMutation());

        assertEquals(SpotSandboxMatchingDecision.RUNTIME_NOT_IMPLEMENTED, design.runtimeStatus());
        assertEquals(
                List.of(
                        SpotSandboxMatchingCapability.MARKET_PARTITION,
                        SpotSandboxMatchingCapability.PRICE_PRIORITY,
                        SpotSandboxMatchingCapability.TIME_PRIORITY,
                        SpotSandboxMatchingCapability.CROSSED_LIMIT_PRICE,
                        SpotSandboxMatchingCapability.PARTIAL_FILL_SEMANTICS,
                        SpotSandboxMatchingCapability.SETTLEMENT_INPUT_BOUNDARY
                ),
                design.capabilities()
        );
        assertEquals(
                List.of(
                        SpotSandboxTradePriceRule.MAKER_PRICE,
                        SpotSandboxTradePriceRule.HUMAN_REVIEW_REQUIRED
                ),
                design.tradePriceRules()
        );
        assertTrue(design.matchingRules().stream().anyMatch(text -> text.contains("matching 是 explicit sandbox process")));
        assertTrue(design.matchingRules().stream().anyMatch(text -> text.contains("matching 必須以 marketSymbol 分區")));
        assertTrue(design.matchingRules().stream().anyMatch(text -> text.contains("BUY limit price >= SELL limit price")));
        assertTrue(design.matchingRules().stream().anyMatch(text -> text.contains("BUY side 高價優先")));
        assertTrue(design.matchingRules().stream().anyMatch(text -> text.contains("SELL side 低價優先")));
        assertTrue(design.matchingRules().stream().anyMatch(text -> text.contains("同價位依 acceptedAt 或 sequence 先後排序")));
        assertTrue(design.matchingRules().stream().anyMatch(text -> text.contains("matchedQuantity = min(buyRemaining, sellRemaining)")));
        assertTrue(design.matchingRules().stream().anyMatch(text -> text.contains("matching result 不等於 settlement completed / ledger posted / balance updated")));
        assertTrue(design.matchingRules().stream().anyMatch(text -> text.contains("matching 必須產生 settlement input")));
        assertTrue(design.matchingRules().stream().anyMatch(text -> text.contains("matching 不得直接寫 ledger、balance_projections 或 reservations")));
        assertTrue(design.matchingRules().stream().anyMatch(text -> text.contains("matching 不得繞過 reservation boundary")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增 matching runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增 trade / fill persistence runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不產生 FILLED / PARTIALLY_FILLED runtime mutation")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不寫 orders / trades / reservations / balance_projections / ledger tables")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("所有 matching / trade / settlement runtime 仍屬 HUMAN_REVIEW_REQUIRED")));
    }
}
