package com.lumix.trading.core.futures.sandbox.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.common.RequestId;
import com.lumix.trading.core.futures.leverage.FuturesLeverage;
import com.lumix.trading.core.futures.order.FuturesOrderId;
import com.lumix.trading.core.futures.order.FuturesOrderPlacementReason;
import com.lumix.trading.core.futures.order.FuturesOrderPlacementResult;
import com.lumix.trading.core.futures.order.FuturesOrderSide;
import com.lumix.trading.core.futures.order.FuturesOrderStatus;
import com.lumix.trading.core.futures.order.FuturesOrderType;
import com.lumix.trading.core.futures.order.FuturesSandboxOrder;
import com.lumix.trading.core.futures.order.FuturesTimeInForce;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPrice;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPriceSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 驗證 T06 只讓同一受限 market 的 accepted order 與人工價格快照進入 inspection，不建立交易狀態。
 */
class FuturesSandboxRestrictedContractGateTest {

    private final FuturesSandboxRestrictedContractGate gate = new FuturesSandboxRestrictedContractGate();

    /**
     * 確認成功結果只保留既有 accepted snapshot 與 mock price，不改變 order 或建立 fill / position。
     */
    @Test
    void makesSameMarketAcceptedOrderEligibleForSandboxInspection() {
        RestrictedFuturesSandboxContract contract = contract("BTC-USDT");
        FuturesOrderPlacementResult placementResult = acceptedOrder("BTC-USDT");
        FuturesSandboxMockMarkPrice markPrice = markPrice("BTC-USDT");

        FuturesSandboxContractEligibilityResult result = gate.evaluate(contract, placementResult, markPrice);

        assertEquals(FuturesSandboxContractEligibilityDecision.ELIGIBLE_FOR_SANDBOX_INSPECTION, result.decision());
        assertEquals(FuturesSandboxContractEligibilityReason.ACCEPTED_ORDER_AND_MOCK_PRICE_WITHIN_CONTRACT, result.reason());
        FuturesSandboxContractInspection inspection = result.inspection().orElseThrow();
        assertEquals(contract, inspection.contract());
        assertEquals(placementResult.acceptedOrder().orElseThrow(), inspection.acceptedOrder());
        assertEquals(markPrice, inspection.markPrice());
        assertEquals(FuturesOrderStatus.ACCEPTED_FOR_SANDBOX, inspection.acceptedOrder().status());
    }

    /**
     * 確認未受理 order、order market 與 mark-price market 均無法跨越單一 contract 範圍。
     */
    @Test
    void rejectsPlacementAndMarketBoundaryViolations() {
        RestrictedFuturesSandboxContract contract = contract("BTC-USDT");

        FuturesSandboxContractEligibilityResult rejectedPlacement = gate.evaluate(
                contract,
                FuturesOrderPlacementResult.rejected(FuturesOrderPlacementReason.ACCOUNT_MISMATCH),
                markPrice("BTC-USDT")
        );
        FuturesSandboxContractEligibilityResult orderMarketMismatch = gate.evaluate(
                contract,
                acceptedOrder("ETH-USDT"),
                markPrice("BTC-USDT")
        );
        FuturesSandboxContractEligibilityResult priceMarketMismatch = gate.evaluate(
                contract,
                acceptedOrder("BTC-USDT"),
                markPrice("ETH-USDT")
        );

        assertRejected(rejectedPlacement, FuturesSandboxContractEligibilityReason.ORDER_NOT_ACCEPTED_FOR_SANDBOX);
        assertRejected(orderMarketMismatch, FuturesSandboxContractEligibilityReason.ORDER_MARKET_OUTSIDE_CONTRACT);
        assertRejected(priceMarketMismatch, FuturesSandboxContractEligibilityReason.MARK_PRICE_MARKET_OUTSIDE_CONTRACT);
    }

    private static RestrictedFuturesSandboxContract contract(String market) {
        return new RestrictedFuturesSandboxContract(new FuturesMarketSymbol(market));
    }

    private static FuturesOrderPlacementResult acceptedOrder(String market) {
        FuturesSandboxOrder order = new FuturesSandboxOrder(
                new FuturesOrderId("order-" + market),
                new RequestId("request-" + market),
                new AccountId("futures-account"),
                new FuturesMarketSymbol(market),
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal("2")),
                new FuturesEntryPrice(new BigDecimal("100")),
                FuturesTimeInForce.GTC,
                FuturesLeverage.of(10),
                Instant.parse("2026-07-20T00:00:00Z"),
                FuturesOrderStatus.ACCEPTED_FOR_SANDBOX,
                Optional.empty()
        );
        return FuturesOrderPlacementResult.accepted(order);
    }

    private static FuturesSandboxMockMarkPrice markPrice(String market) {
        return new FuturesSandboxMockMarkPrice(
                new FuturesMarketSymbol(market),
                new BigDecimal("110"),
                FuturesSandboxMockMarkPriceSource.MANUAL_SANDBOX_INPUT,
                Instant.parse("2026-07-20T00:01:00Z")
        );
    }

    private static void assertRejected(
            FuturesSandboxContractEligibilityResult result,
            FuturesSandboxContractEligibilityReason expectedReason
    ) {
        assertEquals(FuturesSandboxContractEligibilityDecision.REJECTED, result.decision());
        assertEquals(expectedReason, result.reason());
        assertTrue(result.inspection().isEmpty());
    }
}
