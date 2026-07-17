package com.lumix.trading.core.sandbox.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證共用限價單規則保留既有 spot sandbox 的價格、時間與數量優先序。
 */
class SandboxLimitOrderMatchingPolicyTest {

    private final SandboxLimitOrderMatchingPolicy policy = new SandboxLimitOrderMatchingPolicy();

    /**
     * 確認規則選擇最高 BUY、最低 SELL，並使用較早 acceptedAt 的 maker 與最小剩餘數量。
     */
    @Test
    void selectsBestCrossedPairWithPriceTimePriorityAndPartialQuantity() {
        var result = policy.selectBestCrossedPair(List.of(
                candidate("buy-low", SandboxLimitOrderSide.BUY, "100", "9", "2026-07-17T00:02:00Z"),
                candidate("buy-best", SandboxLimitOrderSide.BUY, "102", "7", "2026-07-17T00:03:00Z"),
                candidate("sell-high", SandboxLimitOrderSide.SELL, "101", "8", "2026-07-17T00:01:00Z"),
                candidate("sell-best", SandboxLimitOrderSide.SELL, "99", "3", "2026-07-17T00:04:00Z")
        ));

        SandboxLimitOrderMatchPair pair = result.orElseThrow();
        assertEquals("buy-best", pair.buyOrder().orderId());
        assertEquals("sell-best", pair.sellOrder().orderId());
        assertEquals("buy-best", pair.makerOrder().orderId());
        assertEquals(new BigDecimal("3"), pair.matchedQuantity());
    }

    /**
     * 確認沒有 crossed price 時只能回傳空結果，避免 pure policy 假裝已產生成交。
     */
    @Test
    void returnsEmptyWhenPricesDoNotCross() {
        assertTrue(policy.selectBestCrossedPair(List.of(
                candidate("buy", SandboxLimitOrderSide.BUY, "99", "1", "2026-07-17T00:00:00Z"),
                candidate("sell", SandboxLimitOrderSide.SELL, "100", "1", "2026-07-17T00:01:00Z")
        )).isEmpty());
    }

    private static SandboxLimitOrderCandidate candidate(
            String id,
            SandboxLimitOrderSide side,
            String price,
            String quantity,
            String acceptedAt
    ) {
        return new SandboxLimitOrderCandidate(
                id,
                "BTC-USDT",
                side,
                new BigDecimal(price),
                new BigDecimal(quantity),
                Instant.parse(acceptedAt)
        );
    }
}
