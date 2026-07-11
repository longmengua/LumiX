package com.lumix.trading.core.spot.orderbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Spot sandbox order book 只停在 in-memory boundary，不會被誤解為正式 order book runtime。
 */
class SpotSandboxOrderBookPolicyTest {

    private final SpotSandboxOrderBookPolicy policy = new SpotSandboxOrderBookPolicy();

    /**
     * 確認 order book design contract 明確定義 sandbox-only 與 duplicate protection 邊界。
     *
     * 這個 case 必須存在，因為 in-memory order book 一旦沒鎖住，就很容易被誤接成正式 order book。
     */
    @Test
    void describeReturnsExpectedSpotSandboxOrderBookContract() {
        SpotSandboxOrderBookDesign design = policy.describe();

        assertTrue(policy.requiresSandboxOnly());
        assertTrue(policy.forbidsProductionReadyClaim());
        assertTrue(policy.requiresInMemoryBook());
        assertTrue(policy.requiresIntakeAcceptance());
        assertTrue(policy.requiresDuplicateIdempotencyProtection());
        assertTrue(policy.forbidsFilledStatusesInThisTask());
        assertTrue(policy.forbidsDirectPersistence());
        assertTrue(policy.forbidsBoundaryBypass());

        assertEquals(
                List.of(
                        SpotSandboxOrderStatus.ACCEPTED,
                        SpotSandboxOrderStatus.OPEN,
                        SpotSandboxOrderStatus.REJECTED,
                        SpotSandboxOrderStatus.CANCELLED,
                        SpotSandboxOrderStatus.FILLED,
                        SpotSandboxOrderStatus.PARTIALLY_FILLED
                ),
                design.supportedStatuses()
        );
        assertTrue(design.bookRules().stream().anyMatch(text -> text.contains("in-memory sandbox book only")));
        assertTrue(design.bookRules().stream().anyMatch(text -> text.contains("只接受 P16-T02 accepted intake result 或 accepted command")));
        assertTrue(design.bookRules().stream().anyMatch(text -> text.contains("accepted order book insert 不代表 persisted / reserved / matched / settled / posted")));
        assertTrue(design.bookRules().stream().anyMatch(text -> text.contains("marketSymbol 查詢只回傳該 market 的 open orders")));
        assertTrue(design.bookRules().stream().anyMatch(text -> text.contains("price / quantity / remainingQuantity 一律使用 BigDecimal")));
        assertTrue(design.duplicateRules().stream().anyMatch(text -> text.contains("duplicate idempotencyKey 不得建立第二筆不同 sandboxOrderId")));
        assertTrue(design.duplicateRules().stream().anyMatch(text -> text.contains("duplicate 只能回傳 existing result 或 duplicate rejected")));
        assertTrue(design.duplicateRules().stream().anyMatch(text -> text.contains("rejected intake result 不得進 book")));
        assertTrue(design.duplicateRules().stream().anyMatch(text -> text.contains("本題不得產生 FILLED / PARTIALLY_FILLED")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不新增正式 order placement runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不接 reservation / matching / settlement / ledger / balance projection runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("所有後續 reservation / matching / settlement 仍屬 HUMAN_REVIEW_REQUIRED")));
    }
}
