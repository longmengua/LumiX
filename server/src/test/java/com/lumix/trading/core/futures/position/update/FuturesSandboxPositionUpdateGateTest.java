package com.lumix.trading.core.futures.position.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.lumix.trading.core.futures.position.FuturesPosition;
import com.lumix.trading.core.futures.position.FuturesPositionId;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import com.lumix.trading.core.futures.position.FuturesPositionSide;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 驗證 T03 僅在 verified fill 上建立 one-way、open-only 的 immutable position snapshots。
 */
class FuturesSandboxPositionUpdateGateTest {

    private final FuturesSandboxPositionUpdateGate gate = new FuturesSandboxPositionUpdateGate();

    /**
     * 確認買方只會開 LONG、賣方只會開 SHORT，且 position identity 可由 fill ID deterministic 重建。
     */
    @Test
    void opensOneWayLongAndShortSnapshotsFromVerifiedFill() {
        FuturesSandboxPositionUpdateRequest request = request(Set.of(), Optional.empty(), Optional.empty());

        FuturesSandboxPositionUpdateResult result = gate.evaluate(request);

        assertEquals(FuturesSandboxPositionUpdateDecision.OPENED_FOR_SANDBOX, result.decision());
        assertEquals(FuturesSandboxPositionUpdateReason.OPENED_FROM_VERIFIED_FILL, result.reason());
        FuturesPosition buyerLong = result.buyerLongPosition().orElseThrow();
        FuturesPosition sellerShort = result.sellerShortPosition().orElseThrow();
        assertEquals(new FuturesPositionId("sandbox-position-fill-001-buy"), buyerLong.positionId());
        assertEquals(new AccountId("buyer"), buyerLong.futuresAccountId());
        assertEquals(FuturesPositionSide.LONG, buyerLong.side());
        assertEquals(new FuturesPositionId("sandbox-position-fill-001-sell"), sellerShort.positionId());
        assertEquals(new AccountId("seller"), sellerShort.futuresAccountId());
        assertEquals(FuturesPositionSide.SHORT, sellerShort.side());
        assertEquals(0, new BigDecimal("2").compareTo(buyerLong.quantity().value()));
        assertEquals(0, new BigDecimal("100").compareTo(sellerShort.entryPrice().value()));
        assertEquals(request.verifiedFill().filledAt(), buyerLong.openedAt());
        assertEquals(request.verifiedFill().filledAt(), sellerShort.updatedAt());
    }

    /**
     * 確認 pure gate 依 caller-provided processed fill set 阻擋重送，且不產出半成品 position。
     */
    @Test
    void rejectsPreviouslyProcessedFill() {
        FuturesSandboxPositionUpdateResult result = gate.evaluate(request(
                Set.of(new FuturesSandboxFillId("fill-001")),
                Optional.empty(),
                Optional.empty()
        ));

        assertEquals(FuturesSandboxPositionUpdateDecision.REJECTED, result.decision());
        assertEquals(FuturesSandboxPositionUpdateReason.FILL_ALREADY_PROCESSED, result.reason());
        assertTrue(result.buyerLongPosition().isEmpty());
        assertTrue(result.sellerShortPosition().isEmpty());
    }

    /**
     * 確認 open-only boundary 一律拒絕現有部位，並先檢查該部位是否屬於同一 account / market 範圍。
     */
    @Test
    void rejectsExistingOrOutOfScopePositions() {
        FuturesSandboxPositionUpdateResult existingBuyer = gate.evaluate(request(
                Set.of(),
                Optional.of(existingPosition("buyer", "BTC-USDT", FuturesPositionSide.LONG)),
                Optional.empty()
        ));
        FuturesSandboxPositionUpdateResult outOfScopeSeller = gate.evaluate(request(
                Set.of(),
                Optional.empty(),
                Optional.of(existingPosition("other-seller", "BTC-USDT", FuturesPositionSide.SHORT))
        ));

        assertEquals(FuturesSandboxPositionUpdateReason.BUYER_POSITION_ALREADY_OPEN, existingBuyer.reason());
        assertEquals(FuturesSandboxPositionUpdateReason.SELLER_POSITION_SCOPE_MISMATCH, outOfScopeSeller.reason());
        assertTrue(existingBuyer.buyerLongPosition().isEmpty());
        assertTrue(outOfScopeSeller.sellerShortPosition().isEmpty());
    }

    private static FuturesSandboxPositionUpdateRequest request(
            Set<FuturesSandboxFillId> processedFillIds,
            Optional<FuturesPosition> existingBuyerPosition,
            Optional<FuturesPosition> existingSellerPosition
    ) {
        return new FuturesSandboxPositionUpdateRequest(
                new FuturesSandboxVerifiedFill(
                        new FuturesSandboxFillId("fill-001"),
                        order("buy", "buyer", FuturesOrderSide.BUY, "101", "3"),
                        order("sell", "seller", FuturesOrderSide.SELL, "99", "4"),
                        new FuturesEntryPrice(new BigDecimal("100")),
                        new FuturesPositionQuantity(new BigDecimal("2")),
                        Instant.parse("2026-07-18T01:02:00Z")
                ),
                processedFillIds,
                existingBuyerPosition,
                existingSellerPosition
        );
    }

    private static FuturesPosition existingPosition(String accountId, String marketSymbol, FuturesPositionSide side) {
        return FuturesPosition.open(
                new FuturesPositionId("existing-" + accountId),
                new AccountId(accountId),
                new FuturesMarketSymbol(marketSymbol),
                side,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("100")),
                Instant.parse("2026-07-18T00:59:00Z")
        );
    }

    private static FuturesSandboxOrder order(
            String orderId,
            String accountId,
            FuturesOrderSide side,
            String price,
            String quantity
    ) {
        return new FuturesSandboxOrder(
                new FuturesOrderId(orderId),
                new RequestId("request-" + orderId),
                new AccountId(accountId),
                new FuturesMarketSymbol("BTC-USDT"),
                side,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal(quantity)),
                new FuturesEntryPrice(new BigDecimal(price)),
                FuturesTimeInForce.GTC,
                FuturesLeverage.of(10),
                Instant.parse("2026-07-18T01:00:00Z"),
                FuturesOrderStatus.ACCEPTED_FOR_SANDBOX,
                Optional.empty()
        );
    }
}
