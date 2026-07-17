package com.lumix.trading.core.futures.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.account.AccountStatus;
import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.common.MoneyAmount;
import com.lumix.common.RequestId;
import com.lumix.trading.core.futures.account.FuturesAccount;
import com.lumix.trading.core.futures.account.FuturesMarginMode;
import com.lumix.trading.core.futures.leverage.FuturesLeverage;
import com.lumix.trading.core.futures.leverage.IsolatedLeverageConfig;
import com.lumix.trading.core.futures.margin.IsolatedMarginCheckRequest;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 驗證 futures sandbox placement gate 只做 deterministic 的 in-memory decision，不偷渡其他 runtime。
 */
class FuturesOrderPlacementGateTest {

    private final FuturesOrderPlacementGate gate = new FuturesOrderPlacementGate();

    /**
     * 確認合法 LIMIT / GTC request 會得到 ACCEPTED_FOR_SANDBOX。
     *
     * 這個 case 必須存在，因為 T01 的唯一正向能力就是建立受限 sandbox accepted snapshot。
     */
    @Test
    void acceptsValidLimitGtcRequest() {
        FuturesOrderPlacementRequest request = request(
                "futures-acct-main",
                "BTC-USDT",
                "1",
                "20000",
                "3000",
                10,
                Optional.of("  cli-001  ")
        );

        FuturesOrderPlacementResult result = gate.evaluate(request);

        assertEquals(FuturesOrderStatus.ACCEPTED_FOR_SANDBOX, result.status());
        assertEquals(FuturesOrderPlacementReason.SANDBOX_ORDER_ACCEPTED, result.reason());
        FuturesSandboxOrder acceptedOrder = result.acceptedOrder().orElseThrow();
        assertEquals(request.submittedAt(), acceptedOrder.acceptedAt());
        assertEquals(request.leverageConfig().leverage(), acceptedOrder.leverage());
        assertEquals(Optional.of("cli-001"), acceptedOrder.clientOrderId());
        assertEquals(FuturesOrderStatus.ACCEPTED_FOR_SANDBOX, acceptedOrder.status());
    }

    /**
     * 確認 accepted snapshot 不包含 fill、trade、position、reservation 或 ledger 參考。
     */
    @Test
    void acceptedSnapshotContainsNoRuntimeArtifacts() {
        List<String> components = Arrays.stream(FuturesSandboxOrder.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertFalse(components.contains("filledQuantity"));
        assertFalse(components.contains("averageFillPrice"));
        assertFalse(components.contains("tradeId"));
        assertFalse(components.contains("positionId"));
        assertFalse(components.contains("reservationId"));
        assertFalse(components.contains("ledgerReference"));
        assertFalse(components.contains("settlementReference"));
        assertFalse(components.contains("orderBookSequence"));
    }

    /**
     * 確認 approved margin proposal 不得重用到不同 account。
     */
    @Test
    void rejectsAccountMismatchBetweenOrderAndMarginProposal() {
        FuturesOrderPlacementRequest request = new FuturesOrderPlacementRequest(
                new RequestId("req-001"),
                new FuturesOrderId("fut-order-001"),
                new AccountId("futures-acct-main"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("20000")),
                FuturesTimeInForce.GTC,
                sampleLeverageConfig("futures-acct-main", "BTC-USDT", 10),
                sampleMarginCheckRequest("futures-acct-other", AccountStatus.ACTIVE, "BTC-USDT", "1", "20000", "3000", 10, "USDT", "USDT"),
                Instant.parse("2026-07-16T00:00:00Z"),
                Optional.empty()
        );

        FuturesOrderPlacementResult result = gate.evaluate(request);

        assertEquals(FuturesOrderStatus.REJECTED, result.status());
        assertEquals(FuturesOrderPlacementReason.ACCOUNT_MISMATCH, result.reason());
        assertTrue(result.acceptedOrder().isEmpty());
    }

    /**
     * 確認 approved margin proposal 不得重用到不同 market。
     */
    @Test
    void rejectsMarketMismatchBetweenOrderAndMarginProposal() {
        FuturesOrderPlacementRequest request = new FuturesOrderPlacementRequest(
                new RequestId("req-001"),
                new FuturesOrderId("fut-order-001"),
                new AccountId("futures-acct-main"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("20000")),
                FuturesTimeInForce.GTC,
                sampleLeverageConfig("futures-acct-main", "BTC-USDT", 10),
                sampleMarginCheckRequest("futures-acct-main", AccountStatus.ACTIVE, "ETH-USDT", "1", "20000", "3000", 10, "USDT", "USDT"),
                Instant.parse("2026-07-16T00:00:00Z"),
                Optional.empty()
        );

        FuturesOrderPlacementResult result = gate.evaluate(request);

        assertEquals(FuturesOrderPlacementReason.MARKET_MISMATCH, result.reason());
    }

    /**
     * 確認 approved margin proposal 不得重用到不同 quantity、price 或 leverage config。
     */
    @Test
    void rejectsMarginProposalMismatchAcrossQuantityPriceAndLeverage() {
        FuturesOrderPlacementResult quantityMismatch = gate.evaluate(new FuturesOrderPlacementRequest(
                new RequestId("req-001"),
                new FuturesOrderId("fut-order-001"),
                new AccountId("futures-acct-main"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal("2")),
                new FuturesEntryPrice(new BigDecimal("20000")),
                FuturesTimeInForce.GTC,
                sampleLeverageConfig("futures-acct-main", "BTC-USDT", 10),
                sampleMarginCheckRequest("futures-acct-main", AccountStatus.ACTIVE, "BTC-USDT", "1", "20000", "3000", 10, "USDT", "USDT"),
                Instant.parse("2026-07-16T00:00:00Z"),
                Optional.empty()
        ));
        assertEquals(FuturesOrderPlacementReason.MARGIN_PROPOSAL_MISMATCH, quantityMismatch.reason());

        FuturesOrderPlacementResult priceMismatch = gate.evaluate(new FuturesOrderPlacementRequest(
                new RequestId("req-001"),
                new FuturesOrderId("fut-order-001"),
                new AccountId("futures-acct-main"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("21000")),
                FuturesTimeInForce.GTC,
                sampleLeverageConfig("futures-acct-main", "BTC-USDT", 10),
                sampleMarginCheckRequest("futures-acct-main", AccountStatus.ACTIVE, "BTC-USDT", "1", "20000", "3000", 10, "USDT", "USDT"),
                Instant.parse("2026-07-16T00:00:00Z"),
                Optional.empty()
        ));
        assertEquals(FuturesOrderPlacementReason.MARGIN_PROPOSAL_MISMATCH, priceMismatch.reason());

        FuturesOrderPlacementResult leverageMismatch = gate.evaluate(new FuturesOrderPlacementRequest(
                new RequestId("req-001"),
                new FuturesOrderId("fut-order-001"),
                new AccountId("futures-acct-main"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("20000")),
                FuturesTimeInForce.GTC,
                sampleLeverageConfig("futures-acct-main", "BTC-USDT", 20),
                sampleMarginCheckRequest("futures-acct-main", AccountStatus.ACTIVE, "BTC-USDT", "1", "20000", "3000", 10, "USDT", "USDT"),
                Instant.parse("2026-07-16T00:00:00Z"),
                Optional.empty()
        ));
        assertEquals(FuturesOrderPlacementReason.MARGIN_PROPOSAL_MISMATCH, leverageMismatch.reason());
    }

    /**
     * 確認 margin gate 一旦不是 APPROVED / SUFFICIENT_MARGIN，就映射成 MARGIN_CHECK_NOT_APPROVED。
     */
    @Test
    void rejectsWhenRecomputedMarginResultIsNotApproved() {
        FuturesOrderPlacementResult insufficient = gate.evaluate(request(
                "futures-acct-main",
                "BTC-USDT",
                "1",
                "20000",
                "100",
                1,
                Optional.empty()
        ));
        assertEquals(FuturesOrderPlacementReason.MARGIN_CHECK_NOT_APPROVED, insufficient.reason());

        FuturesOrderPlacementResult accountNotActive = gate.evaluate(new FuturesOrderPlacementRequest(
                new RequestId("req-001"),
                new FuturesOrderId("fut-order-001"),
                new AccountId("futures-acct-main"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("20000")),
                FuturesTimeInForce.GTC,
                sampleLeverageConfig("futures-acct-main", "BTC-USDT", 10),
                sampleMarginCheckRequest("futures-acct-main", AccountStatus.FROZEN, "BTC-USDT", "1", "20000", "3000", 10, "USDT", "USDT"),
                Instant.parse("2026-07-16T00:00:00Z"),
                Optional.empty()
        ));
        assertEquals(FuturesOrderPlacementReason.MARGIN_CHECK_NOT_APPROVED, accountNotActive.reason());

        FuturesOrderPlacementResult internalAccountMismatch = gate.evaluate(new FuturesOrderPlacementRequest(
                new RequestId("req-001"),
                new FuturesOrderId("fut-order-001"),
                new AccountId("futures-acct-main"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("20000")),
                FuturesTimeInForce.GTC,
                sampleLeverageConfig("futures-acct-main", "BTC-USDT", 10),
                sampleMarginCheckRequest("futures-acct-main", AccountStatus.ACTIVE, "BTC-USDT", "1", "20000", "3000", 20, "USDT", "USDT"),
                Instant.parse("2026-07-16T00:00:00Z"),
                Optional.empty()
        ));
        assertEquals(FuturesOrderPlacementReason.MARGIN_PROPOSAL_MISMATCH, internalAccountMismatch.reason());

        FuturesOrderPlacementResult internalMarketMismatch = gate.evaluate(new FuturesOrderPlacementRequest(
                new RequestId("req-001"),
                new FuturesOrderId("fut-order-001"),
                new AccountId("futures-acct-main"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("20000")),
                FuturesTimeInForce.GTC,
                sampleLeverageConfig("futures-acct-main", "BTC-USDT", 10),
                sampleMarginCheckRequest(
                        sampleAccount("futures-acct-main", AccountStatus.ACTIVE, "USDT"),
                        sampleLeverageConfig("futures-acct-main", "ETH-USDT", 10),
                        new FuturesMarketSymbol("BTC-USDT"),
                        "1",
                        "20000",
                        "3000",
                        "USDT"
                ),
                Instant.parse("2026-07-16T00:00:00Z"),
                Optional.empty()
        ));
        assertEquals(FuturesOrderPlacementReason.MARGIN_PROPOSAL_MISMATCH, internalMarketMismatch.reason());

        FuturesOrderPlacementResult settlementMismatch = gate.evaluate(new FuturesOrderPlacementRequest(
                new RequestId("req-001"),
                new FuturesOrderId("fut-order-001"),
                new AccountId("futures-acct-main"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("20000")),
                FuturesTimeInForce.GTC,
                sampleLeverageConfig("futures-acct-main", "BTC-USDT", 10),
                sampleMarginCheckRequest("futures-acct-main", AccountStatus.ACTIVE, "BTC-USDT", "1", "20000", "3000", 10, "USDT", "USDC"),
                Instant.parse("2026-07-16T00:00:00Z"),
                Optional.empty()
        ));
        assertEquals(FuturesOrderPlacementReason.MARGIN_CHECK_NOT_APPROVED, settlementMismatch.reason());
    }

    /**
     * 確認相同輸入重複執行會得到相同結果，且 gate 不修改任何輸入。
     */
    @Test
    void gateIsDeterministicAndDoesNotMutateInputs() {
        FuturesOrderPlacementRequest request = request(
                "futures-acct-main",
                "BTC-USDT",
                "1",
                "20000",
                "3000",
                10,
                Optional.empty()
        );
        IsolatedLeverageConfig originalConfig = request.leverageConfig();
        IsolatedMarginCheckRequest originalMarginRequest = request.marginCheckRequest();

        FuturesOrderPlacementResult first = gate.evaluate(request);
        FuturesOrderPlacementResult second = gate.evaluate(request);

        assertEquals(first, second);
        assertEquals(originalConfig, request.leverageConfig());
        assertEquals(originalMarginRequest, request.marginCheckRequest());
        assertNotSame(first, second);
    }

    /**
     * 確認 gate 不接受 null request。
     */
    @Test
    void gateRejectsNullRequest() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> gate.evaluate(null));
        assertEquals("request must not be null", exception.getMessage());
    }

    private static FuturesOrderPlacementRequest request(
            String accountId,
            String marketSymbol,
            String quantity,
            String price,
            String availableMargin,
            int leverageMultiplier,
            Optional<String> clientOrderId
    ) {
        return new FuturesOrderPlacementRequest(
                new RequestId("req-001"),
                new FuturesOrderId("fut-order-001"),
                new AccountId(accountId),
                new FuturesMarketSymbol(marketSymbol),
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal(quantity)),
                new FuturesEntryPrice(new BigDecimal(price)),
                FuturesTimeInForce.GTC,
                sampleLeverageConfig(accountId, marketSymbol, leverageMultiplier),
                sampleMarginCheckRequest(accountId, AccountStatus.ACTIVE, marketSymbol, quantity, price, availableMargin, leverageMultiplier, "USDT", "USDT"),
                Instant.parse("2026-07-16T00:00:00Z"),
                clientOrderId
        );
    }

    private static FuturesAccount sampleAccount(String accountId, AccountStatus status, String settlementAsset) {
        return new FuturesAccount(
                new AccountId(accountId),
                new UserId("user-" + accountId),
                status,
                FuturesMarginMode.ISOLATED,
                new AssetSymbol(settlementAsset),
                Instant.parse("2026-07-15T03:04:05Z"),
                Instant.parse("2026-07-15T03:04:06Z")
        );
    }

    private static IsolatedLeverageConfig sampleLeverageConfig(String accountId, String marketSymbol, int leverageMultiplier) {
        return IsolatedLeverageConfig.configure(
                new AccountId(accountId),
                new FuturesMarketSymbol(marketSymbol),
                FuturesLeverage.of(leverageMultiplier),
                Instant.parse("2026-07-15T03:04:07Z")
        );
    }

    private static IsolatedMarginCheckRequest sampleMarginCheckRequest(
            String accountId,
            AccountStatus accountStatus,
            String marketSymbol,
            String quantity,
            String entryPrice,
            String availableMargin,
            int leverageMultiplier,
            String settlementAsset,
            String availableMarginAsset
    ) {
        return sampleMarginCheckRequest(
                sampleAccount(accountId, accountStatus, settlementAsset),
                sampleLeverageConfig(accountId, marketSymbol, leverageMultiplier),
                new FuturesMarketSymbol(marketSymbol),
                quantity,
                entryPrice,
                availableMargin,
                availableMarginAsset
        );
    }

    private static IsolatedMarginCheckRequest sampleMarginCheckRequest(
            FuturesAccount account,
            IsolatedLeverageConfig leverageConfig,
            FuturesMarketSymbol marketSymbol,
            String quantity,
            String entryPrice,
            String availableMargin,
            String availableMarginAsset
    ) {
        return new IsolatedMarginCheckRequest(
                account,
                leverageConfig,
                marketSymbol,
                new FuturesPositionQuantity(new BigDecimal(quantity)),
                new FuturesEntryPrice(new BigDecimal(entryPrice)),
                new AssetSymbol(availableMarginAsset),
                new MoneyAmount(new BigDecimal(availableMargin))
        );
    }
}
