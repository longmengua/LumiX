package com.lumix.trading.core.futures.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.common.MoneyAmount;
import com.lumix.common.RequestId;
import com.lumix.trading.core.futures.account.FuturesAccount;
import com.lumix.trading.core.futures.leverage.FuturesLeverage;
import com.lumix.trading.core.futures.leverage.IsolatedLeverageConfig;
import com.lumix.trading.core.futures.margin.IsolatedMarginCheckRequest;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 驗證 futures order placement request 只能承接完整且不可變的 sandbox 輸入快照。
 */
class FuturesOrderPlacementRequestTest {

    /**
     * 確認合法 request 可以保留 order placement 與 margin proposal 的完整輸入。
     *
     * 這個 case 必須存在，因為 T01 的 gate 只允許依賴明確快照，不可偷偷查外部狀態。
     */
    @Test
    void constructorAcceptsValidRequestAndNormalizesOptionalClientOrderId() {
        FuturesOrderPlacementRequest request = validRequest(Optional.of("  cli-001  "));

        assertEquals(new FuturesOrderId("fut-order-001"), request.orderId());
        assertEquals(Optional.of("cli-001"), request.clientOrderId());
        assertEquals(FuturesOrderType.LIMIT, request.type());
        assertEquals(FuturesTimeInForce.GTC, request.timeInForce());
    }

    /**
     * 確認 Optional.empty() 是合法 clientOrderId。
     */
    @Test
    void constructorAcceptsEmptyClientOrderIdOptional() {
        FuturesOrderPlacementRequest request = validRequest(Optional.empty());

        assertTrue(request.clientOrderId().isEmpty());
    }

    /**
     * 確認必要欄位與 clientOrderId optional 都不能為 null。
     */
    @Test
    void constructorRejectsNullMandatoryFields() {
        FuturesOrderPlacementRequest request = validRequest(Optional.empty());

        assertThrows(NullPointerException.class, () -> new FuturesOrderPlacementRequest(
                null,
                request.orderId(),
                request.futuresAccountId(),
                request.marketSymbol(),
                request.side(),
                request.type(),
                request.quantity(),
                request.limitPrice(),
                request.timeInForce(),
                request.leverageConfig(),
                request.marginCheckRequest(),
                request.submittedAt(),
                request.clientOrderId()
        ));
        assertThrows(NullPointerException.class, () -> new FuturesOrderPlacementRequest(
                request.requestId(),
                null,
                request.futuresAccountId(),
                request.marketSymbol(),
                request.side(),
                request.type(),
                request.quantity(),
                request.limitPrice(),
                request.timeInForce(),
                request.leverageConfig(),
                request.marginCheckRequest(),
                request.submittedAt(),
                request.clientOrderId()
        ));
        assertThrows(NullPointerException.class, () -> new FuturesOrderPlacementRequest(
                request.requestId(),
                request.orderId(),
                request.futuresAccountId(),
                request.marketSymbol(),
                request.side(),
                request.type(),
                request.quantity(),
                request.limitPrice(),
                request.timeInForce(),
                request.leverageConfig(),
                request.marginCheckRequest(),
                request.submittedAt(),
                null
        ));
    }

    /**
     * 確認 blank clientOrderId 會被拒絕，避免 Optional 裡包住無意義空白。
     */
    @Test
    void constructorRejectsBlankClientOrderIdWhenPresent() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validRequest(Optional.of("   "))
        );

        assertEquals("clientOrderId must not be blank when present", exception.getMessage());
    }

    /**
     * 確認 request 只接受與 leverage config 一致的 account / market。
     *
     * 這個 case 必須存在，因為 order placement request 自己就不能形成跨 account 或跨 market 的半成品。
     */
    @Test
    void constructorRejectsLeverageConfigOwnershipMismatch() {
        IllegalArgumentException accountMismatch = assertThrows(
                IllegalArgumentException.class,
                () -> new FuturesOrderPlacementRequest(
                        new RequestId("req-001"),
                        new FuturesOrderId("fut-order-001"),
                        new AccountId("futures-acct-001"),
                        new FuturesMarketSymbol("BTC-USDT"),
                        FuturesOrderSide.BUY,
                        FuturesOrderType.LIMIT,
                        new FuturesPositionQuantity(new BigDecimal("1")),
                        new FuturesEntryPrice(new BigDecimal("20000")),
                        FuturesTimeInForce.GTC,
                        sampleLeverageConfig("futures-acct-999", "BTC-USDT", 10),
                        sampleMarginCheckRequest("futures-acct-001", "BTC-USDT", "1", "20000", "3000", 10),
                        Instant.parse("2026-07-15T11:00:00Z"),
                        Optional.empty()
                )
        );
        assertEquals("futuresAccountId must equal leverageConfig.futuresAccountId", accountMismatch.getMessage());

        IllegalArgumentException marketMismatch = assertThrows(
                IllegalArgumentException.class,
                () -> new FuturesOrderPlacementRequest(
                        new RequestId("req-001"),
                        new FuturesOrderId("fut-order-001"),
                        new AccountId("futures-acct-001"),
                        new FuturesMarketSymbol("BTC-USDT"),
                        FuturesOrderSide.BUY,
                        FuturesOrderType.LIMIT,
                        new FuturesPositionQuantity(new BigDecimal("1")),
                        new FuturesEntryPrice(new BigDecimal("20000")),
                        FuturesTimeInForce.GTC,
                        sampleLeverageConfig("futures-acct-001", "ETH-USDT", 10),
                        sampleMarginCheckRequest("futures-acct-001", "BTC-USDT", "1", "20000", "3000", 10),
                        Instant.parse("2026-07-15T11:00:00Z"),
                        Optional.empty()
                )
        );
        assertEquals("marketSymbol must equal leverageConfig.marketSymbol", marketMismatch.getMessage());
    }

    /**
     * 確認 T01 的 type boundary 與 time-in-force boundary 都固定成單值 enum。
     */
    @Test
    void typeAndTimeInForceSupportSingleValueOnly() {
        assertEquals(List.of(FuturesOrderType.LIMIT), List.of(FuturesOrderType.values()));
        assertEquals(List.of(FuturesTimeInForce.GTC), List.of(FuturesTimeInForce.values()));
    }

    private static FuturesOrderPlacementRequest validRequest(Optional<String> clientOrderId) {
        return new FuturesOrderPlacementRequest(
                new RequestId("req-001"),
                new FuturesOrderId("fut-order-001"),
                new AccountId("futures-acct-001"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("20000")),
                FuturesTimeInForce.GTC,
                sampleLeverageConfig("futures-acct-001", "BTC-USDT", 10),
                sampleMarginCheckRequest("futures-acct-001", "BTC-USDT", "1", "20000", "3000", 10),
                Instant.parse("2026-07-15T11:00:00Z"),
                clientOrderId
        );
    }

    private static IsolatedLeverageConfig sampleLeverageConfig(String accountId, String marketSymbol, int leverageMultiplier) {
        return IsolatedLeverageConfig.configure(
                new AccountId(accountId),
                new FuturesMarketSymbol(marketSymbol),
                FuturesLeverage.of(leverageMultiplier),
                Instant.parse("2026-07-15T10:59:59Z")
        );
    }

    private static IsolatedMarginCheckRequest sampleMarginCheckRequest(
            String accountId,
            String marketSymbol,
            String quantity,
            String entryPrice,
            String availableMargin,
            int leverageMultiplier
    ) {
        FuturesAccount account = FuturesAccount.open(
                new AccountId(accountId),
                new UserId("user-" + accountId),
                new AssetSymbol("USDT"),
                Instant.parse("2026-07-15T10:59:58Z")
        );
        return new IsolatedMarginCheckRequest(
                account,
                sampleLeverageConfig(accountId, marketSymbol, leverageMultiplier),
                new FuturesMarketSymbol(marketSymbol),
                new FuturesPositionQuantity(new BigDecimal(quantity)),
                new FuturesEntryPrice(new BigDecimal(entryPrice)),
                new AssetSymbol("USDT"),
                new MoneyAmount(new BigDecimal(availableMargin))
        );
    }
}
