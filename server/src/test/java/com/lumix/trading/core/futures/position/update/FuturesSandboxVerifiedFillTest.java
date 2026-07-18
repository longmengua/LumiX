package com.lumix.trading.core.futures.position.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 驗證 T03 verified fill 只接受可作為 position opening 依據的完整 sandbox 事實快照。
 */
class FuturesSandboxVerifiedFillTest {

    /**
     * 確認合法 BUY / SELL accepted orders 可以建立具有獨立 fill price 與 quantity 的輸入快照。
     */
    @Test
    void createsVerifiedFillWithinOrderLimits() {
        FuturesSandboxVerifiedFill fill = fill("100", "2");

        assertEquals(new FuturesSandboxFillId("fill-001"), fill.fillId());
        assertEquals(0, new BigDecimal("100").compareTo(fill.fillPrice().value()));
        assertEquals(0, new BigDecimal("2").compareTo(fill.fillQuantity().value()));
        assertEquals(Instant.parse("2026-07-18T01:02:00Z"), fill.filledAt());
    }

    /**
     * 確認 fill 不得越過任一側限價，或以同一 futures account 同時作為買賣方。
     */
    @Test
    void rejectsOutOfLimitPriceAndSelfMatch() {
        IllegalArgumentException outOfLimit = assertThrows(IllegalArgumentException.class, () -> new FuturesSandboxVerifiedFill(
                new FuturesSandboxFillId("fill-out-of-limit"),
                order("buy", "buyer", FuturesOrderSide.BUY, "100", "2"),
                order("sell", "seller", FuturesOrderSide.SELL, "99", "2"),
                new FuturesEntryPrice(new BigDecimal("101")),
                new FuturesPositionQuantity(new BigDecimal("1")),
                Instant.parse("2026-07-18T01:02:00Z")
        ));
        IllegalArgumentException selfMatch = assertThrows(IllegalArgumentException.class, () -> new FuturesSandboxVerifiedFill(
                new FuturesSandboxFillId("fill-self-match"),
                order("buy-self", "same", FuturesOrderSide.BUY, "100", "2"),
                order("sell-self", "same", FuturesOrderSide.SELL, "99", "2"),
                new FuturesEntryPrice(new BigDecimal("100")),
                new FuturesPositionQuantity(new BigDecimal("1")),
                Instant.parse("2026-07-18T01:02:00Z")
        ));

        assertEquals("fillPrice must remain within both order limits", outOfLimit.getMessage());
        assertEquals("verified fill must not self-match one futures account", selfMatch.getMessage());
    }

    private static FuturesSandboxVerifiedFill fill(String price, String quantity) {
        return new FuturesSandboxVerifiedFill(
                new FuturesSandboxFillId("fill-001"),
                order("buy", "buyer", FuturesOrderSide.BUY, "101", "3"),
                order("sell", "seller", FuturesOrderSide.SELL, "99", "4"),
                new FuturesEntryPrice(new BigDecimal(price)),
                new FuturesPositionQuantity(new BigDecimal(quantity)),
                Instant.parse("2026-07-18T01:02:00Z")
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
