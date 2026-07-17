package com.lumix.trading.core.futures.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.common.RequestId;
import com.lumix.trading.core.futures.leverage.FuturesLeverage;
import com.lumix.trading.core.futures.order.FuturesOrderId;
import com.lumix.trading.core.futures.order.FuturesOrderSide;
import com.lumix.trading.core.futures.order.FuturesOrderStatus;
import com.lumix.trading.core.futures.order.FuturesOrderType;
import com.lumix.trading.core.futures.order.FuturesSandboxOrder;
import com.lumix.trading.core.futures.order.FuturesTimeInForce;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 驗證 P18-T02 只評估 immutable accepted order snapshots，不執行 futures matching runtime。
 */
class FuturesSandboxMatchingGateTest {

    private final FuturesSandboxMatchingGate gate = new FuturesSandboxMatchingGate();

    /**
     * 確認 futures 會重用共用價時優先規則，但結果只停在候選配對，不建立 fill 或 position。
     */
    @Test
    void returnsBestCrossedCandidateWithoutMutatingAcceptedOrders() {
        FuturesSandboxOrder buyLower = order("buy-lower", FuturesOrderSide.BUY, "100", "8", "2026-07-17T00:02:00Z", "BTC-USDT");
        FuturesSandboxOrder buyBest = order("buy-best", FuturesOrderSide.BUY, "102", "7", "2026-07-17T00:03:00Z", "BTC-USDT");
        FuturesSandboxOrder sellHigher = order("sell-higher", FuturesOrderSide.SELL, "101", "6", "2026-07-17T00:01:00Z", "BTC-USDT");
        FuturesSandboxOrder sellBest = order("sell-best", FuturesOrderSide.SELL, "99", "3", "2026-07-17T00:04:00Z", "BTC-USDT");

        FuturesSandboxMatchingResult result = gate.evaluate(
                new FuturesMarketSymbol("BTC-USDT"),
                List.of(buyLower, buyBest, sellHigher, sellBest)
        );

        assertEquals(FuturesSandboxMatchingDecision.MATCH_ELIGIBLE, result.decision());
        assertEquals(FuturesSandboxMatchingReason.CROSSED_LIMIT_PRICE, result.reason());
        FuturesSandboxMatchCandidate candidate = result.candidate().orElseThrow();
        assertEquals("buy-best", candidate.buyOrder().orderId().value());
        assertEquals("sell-best", candidate.sellOrder().orderId().value());
        assertEquals("buy-best", candidate.makerOrder().orderId().value());
        assertEquals(new BigDecimal("3"), candidate.matchedQuantity());
        assertEquals(FuturesOrderStatus.ACCEPTED_FOR_SANDBOX, buyBest.status());
        assertEquals(FuturesOrderStatus.ACCEPTED_FOR_SANDBOX, sellBest.status());
    }

    /**
     * 確認未跨價只能回報 NO_CROSS，不能偽造 trade、fill 或下一階段 position update。
     */
    @Test
    void returnsNoCrossWithoutCandidate() {
        FuturesSandboxMatchingResult result = gate.evaluate(
                new FuturesMarketSymbol("BTC-USDT"),
                List.of(
                        order("buy", FuturesOrderSide.BUY, "99", "1", "2026-07-17T00:00:00Z", "BTC-USDT"),
                        order("sell", FuturesOrderSide.SELL, "100", "1", "2026-07-17T00:01:00Z", "BTC-USDT")
                )
        );

        assertEquals(FuturesSandboxMatchingDecision.NO_CROSS, result.decision());
        assertEquals(FuturesSandboxMatchingReason.NO_CROSSED_ORDERS, result.reason());
        assertTrue(result.candidate().isEmpty());
    }

    /**
     * 確認單一 futures sandbox evaluation 不得混用不同 market 或重複 order identity。
     */
    @Test
    void rejectsMixedMarketAndDuplicateOrderIds() {
        FuturesSandboxMatchingResult mixedMarket = gate.evaluate(
                new FuturesMarketSymbol("BTC-USDT"),
                List.of(
                        order("buy", FuturesOrderSide.BUY, "101", "1", "2026-07-17T00:00:00Z", "BTC-USDT"),
                        order("sell", FuturesOrderSide.SELL, "100", "1", "2026-07-17T00:01:00Z", "ETH-USDT")
                )
        );
        FuturesSandboxMatchingResult duplicateId = gate.evaluate(
                new FuturesMarketSymbol("BTC-USDT"),
                List.of(
                        order("same", FuturesOrderSide.BUY, "101", "1", "2026-07-17T00:00:00Z", "BTC-USDT"),
                        order("same", FuturesOrderSide.SELL, "100", "1", "2026-07-17T00:01:00Z", "BTC-USDT")
                )
        );

        assertEquals(FuturesSandboxMatchingReason.MARKET_MISMATCH, mixedMarket.reason());
        assertEquals(FuturesSandboxMatchingReason.DUPLICATE_ORDER_ID, duplicateId.reason());
        assertFalse(mixedMarket.candidate().isPresent());
        assertFalse(duplicateId.candidate().isPresent());
    }

    private static FuturesSandboxOrder order(
            String orderId,
            FuturesOrderSide side,
            String price,
            String quantity,
            String acceptedAt,
            String marketSymbol
    ) {
        return new FuturesSandboxOrder(
                new FuturesOrderId(orderId),
                new RequestId("request-" + orderId + "-" + side),
                new AccountId("futures-account-" + orderId + "-" + side),
                new FuturesMarketSymbol(marketSymbol),
                side,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal(quantity)),
                new FuturesEntryPrice(new BigDecimal(price)),
                FuturesTimeInForce.GTC,
                FuturesLeverage.of(10),
                Instant.parse(acceptedAt),
                FuturesOrderStatus.ACCEPTED_FOR_SANDBOX,
                Optional.empty()
        );
    }
}
