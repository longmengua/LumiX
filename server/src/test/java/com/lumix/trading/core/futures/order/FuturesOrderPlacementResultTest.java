package com.lumix.trading.core.futures.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.common.RequestId;
import com.lumix.trading.core.futures.leverage.FuturesLeverage;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 驗證 futures order placement result 與 accepted snapshot 的 invariant 不會互相矛盾。
 */
class FuturesOrderPlacementResultTest {

    /**
     * 確認 accepted result 必須攜帶 immutable accepted order snapshot。
     */
    @Test
    void acceptedFactoryCreatesValidAcceptedResult() {
        FuturesSandboxOrder order = acceptedOrder(Optional.of("cli-001"));

        FuturesOrderPlacementResult result = FuturesOrderPlacementResult.accepted(order);

        assertEquals(FuturesOrderStatus.ACCEPTED_FOR_SANDBOX, result.status());
        assertEquals(FuturesOrderPlacementReason.SANDBOX_ORDER_ACCEPTED, result.reason());
        assertEquals(order, result.acceptedOrder().orElseThrow());
        assertEquals(order.acceptedAt(), result.acceptedOrder().orElseThrow().acceptedAt());
    }

    /**
     * 確認 rejected result 不會攜帶 accepted order snapshot。
     */
    @Test
    void rejectedFactoryCreatesValidRejectedResult() {
        FuturesOrderPlacementResult result = FuturesOrderPlacementResult.rejected(FuturesOrderPlacementReason.MARGIN_CHECK_NOT_APPROVED);

        assertEquals(FuturesOrderStatus.REJECTED, result.status());
        assertEquals(FuturesOrderPlacementReason.MARGIN_CHECK_NOT_APPROVED, result.reason());
        assertTrue(result.acceptedOrder().isEmpty());
    }

    /**
     * 確認 canonical constructor 不接受矛盾的 decision / reason / snapshot。
     */
    @Test
    void constructorRejectsInconsistentDecisionAndSnapshot() {
        FuturesSandboxOrder acceptedOrder = acceptedOrder(Optional.empty());

        IllegalArgumentException acceptedWrongReason = assertThrows(
                IllegalArgumentException.class,
                () -> new FuturesOrderPlacementResult(
                        FuturesOrderStatus.ACCEPTED_FOR_SANDBOX,
                        FuturesOrderPlacementReason.ACCOUNT_MISMATCH,
                        Optional.of(acceptedOrder)
                )
        );
        assertEquals("ACCEPTED_FOR_SANDBOX status must use SANDBOX_ORDER_ACCEPTED reason", acceptedWrongReason.getMessage());

        IllegalArgumentException acceptedWithoutOrder = assertThrows(
                IllegalArgumentException.class,
                () -> new FuturesOrderPlacementResult(
                        FuturesOrderStatus.ACCEPTED_FOR_SANDBOX,
                        FuturesOrderPlacementReason.SANDBOX_ORDER_ACCEPTED,
                        Optional.empty()
                )
        );
        assertEquals("ACCEPTED_FOR_SANDBOX result must include acceptedOrder", acceptedWithoutOrder.getMessage());

        IllegalArgumentException rejectedWithAcceptedReason = assertThrows(
                IllegalArgumentException.class,
                () -> new FuturesOrderPlacementResult(
                        FuturesOrderStatus.REJECTED,
                        FuturesOrderPlacementReason.SANDBOX_ORDER_ACCEPTED,
                        Optional.empty()
                )
        );
        assertEquals("REJECTED result must not use SANDBOX_ORDER_ACCEPTED reason", rejectedWithAcceptedReason.getMessage());

        IllegalArgumentException rejectedWithOrder = assertThrows(
                IllegalArgumentException.class,
                () -> new FuturesOrderPlacementResult(
                        FuturesOrderStatus.REJECTED,
                        FuturesOrderPlacementReason.ACCOUNT_MISMATCH,
                        Optional.of(acceptedOrder)
                )
        );
        assertEquals("REJECTED result must not include acceptedOrder", rejectedWithOrder.getMessage());
    }

    /**
     * 確認 accepted snapshot 本身也不能被 constructor 繞過。
     */
    @Test
    void acceptedOrderConstructorRejectsInvalidStatusAndBlankClientOrderId() {
        IllegalArgumentException invalidStatus = assertThrows(
                IllegalArgumentException.class,
                () -> new FuturesSandboxOrder(
                        new FuturesOrderId("fut-order-001"),
                        new RequestId("req-001"),
                        new AccountId("futures-acct-001"),
                        new FuturesMarketSymbol("BTC-USDT"),
                        FuturesOrderSide.BUY,
                        FuturesOrderType.LIMIT,
                        new FuturesPositionQuantity(new BigDecimal("1")),
                        new FuturesEntryPrice(new BigDecimal("20000")),
                        FuturesTimeInForce.GTC,
                        FuturesLeverage.of(10),
                        Instant.parse("2026-07-15T11:00:00Z"),
                        FuturesOrderStatus.REJECTED,
                        Optional.empty()
                )
        );
        assertEquals("accepted sandbox order status must be ACCEPTED_FOR_SANDBOX", invalidStatus.getMessage());

        IllegalArgumentException blankClientOrderId = assertThrows(
                IllegalArgumentException.class,
                () -> acceptedOrder(Optional.of("   "))
        );
        assertEquals("clientOrderId must not be blank when present", blankClientOrderId.getMessage());
    }

    private static FuturesSandboxOrder acceptedOrder(Optional<String> clientOrderId) {
        return new FuturesSandboxOrder(
                new FuturesOrderId("fut-order-001"),
                new RequestId("req-001"),
                new AccountId("futures-acct-001"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("20000")),
                FuturesTimeInForce.GTC,
                FuturesLeverage.of(10),
                Instant.parse("2026-07-15T11:00:00Z"),
                FuturesOrderStatus.ACCEPTED_FOR_SANDBOX,
                clientOrderId
        );
    }
}
