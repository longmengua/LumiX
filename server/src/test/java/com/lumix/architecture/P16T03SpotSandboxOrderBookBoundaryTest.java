package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.trading.core.spot.orderbook.InMemorySpotSandboxOrderBook;
import com.lumix.trading.core.spot.orderbook.SpotSandboxOrderBookBoundary;
import com.lumix.trading.core.spot.orderbook.SpotSandboxOrderBookDecision;
import com.lumix.trading.core.spot.orderbook.SpotSandboxOrderBookRejectionReason;
import com.lumix.trading.core.spot.orderbook.SpotSandboxOrderBookResult;
import com.lumix.trading.core.spot.orderbook.SpotSandboxOrderStatus;
import com.lumix.trading.core.spot.orderbook.SpotSandboxOrderRecord;
import com.lumix.trading.core.spot.orderintake.SpotOrderSide;
import com.lumix.trading.core.spot.orderintake.SpotOrderType;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderCommand;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderIntakeBoundary;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderIntakeResult;
import com.lumix.trading.core.spot.orderintake.SpotTimeInForce;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Spot sandbox in-memory order book gate 只保留 sandbox record，不會變成正式 order runtime。
 */
class P16T03SpotSandboxOrderBookBoundaryTest {

    private final SpotSandboxOrderBookBoundary boundary = new SpotSandboxOrderBookBoundary();
    private final SpotSandboxOrderIntakeBoundary intakeBoundary = new SpotSandboxOrderIntakeBoundary();

    /**
     * 確認 valid LIMIT GTC BUY order 可以進 in-memory book。
     *
     * 這個 case 必須存在，因為 sandbox order book 的第一個正向路徑必須是可重現、可審核的。
     */
    @Test
    void acceptsValidLimitGtcBuyOrderIntoInMemoryBook() {
        Instant acceptedAt = Instant.parse("2026-07-12T01:00:00Z");
        SpotSandboxOrderBookResult result = boundary.accept(validCommand(SpotOrderSide.BUY), acceptedAt);

        assertEquals(SpotSandboxOrderBookDecision.ACCEPTED, result.decision());
        assertTrue(result.rejectionReason() == null);
        assertEquals("sandbox-order-1", result.record().sandboxOrderId());
        assertEquals(SpotSandboxOrderStatus.OPEN, result.record().status());
        assertEquals(acceptedAt, result.record().acceptedAt());
        assertEquals(new BigDecimal("50000.00"), result.record().price());
        assertEquals(new BigDecimal("0.10"), result.record().remainingQuantity());
        assertFalse(result.toString().contains("persisted"));
        assertFalse(result.toString().contains("reserved"));
        assertFalse(result.toString().contains("matched"));
        assertFalse(result.toString().contains("settled"));
        assertFalse(result.toString().contains("posted"));
    }

    /**
     * 確認 valid LIMIT GTC SELL order 可以進 in-memory book。
     *
     * 這個 case 必須存在，因為 BUY / SELL 兩個方向都要能通過 order book materialization。
     */
    @Test
    void acceptsValidLimitGtcSellOrderIntoInMemoryBook() {
        Instant acceptedAt = Instant.parse("2026-07-12T01:05:00Z");
        SpotSandboxOrderBookResult result = boundary.accept(validCommand(SpotOrderSide.SELL), acceptedAt);

        assertEquals(SpotSandboxOrderBookDecision.ACCEPTED, result.decision());
        assertEquals("sandbox-order-1", result.record().sandboxOrderId());
        assertEquals(SpotOrderSide.SELL, result.record().side());
        assertEquals(SpotSandboxOrderStatus.OPEN, result.record().status());
    }

    /**
     * 確認 rejected intake result 不得進 book。
     *
     * 這個 case 必須存在，因為 book 只應接受已通過 P16-T02 的 sandbox command。
     */
    @Test
    void rejectedIntakeResultDoesNotEnterBook() {
        SpotSandboxOrderIntakeResult rejectedIntake = intakeBoundary.evaluate(new SpotSandboxOrderCommand(
                "req-p16-30",
                "idem-p16-30",
                "user-p16-30",
                "acct-p16-30",
                "BTC-USDT",
                SpotOrderSide.BUY,
                SpotOrderType.MARKET,
                new BigDecimal("50000.00"),
                new BigDecimal("0.10"),
                SpotTimeInForce.GTC
        ));

        SpotSandboxOrderBookResult result = boundary.accept(rejectedIntake, Instant.parse("2026-07-12T01:10:00Z"));

        assertEquals(SpotSandboxOrderBookDecision.REJECTED, result.decision());
        assertTrue(result.message().contains("LIMIT"));
        assertEquals(SpotSandboxOrderBookRejectionReason.INTAKE_REJECTED, result.rejectionReason());
    }

    /**
     * 確認 duplicate idempotencyKey 不得建立第二筆不同 sandboxOrderId。
     *
     * 這個 case 必須存在，因為 in-memory book 至少要有保守 duplicate protection，不能在同一個 book 內自我重複。
     */
    @Test
    void duplicateIdempotencyKeyReturnsExistingRecordWithoutNewSandboxOrderId() {
        Instant firstAcceptedAt = Instant.parse("2026-07-12T01:15:00Z");
        Instant secondAcceptedAt = Instant.parse("2026-07-12T01:16:00Z");

        SpotSandboxOrderBookResult first = boundary.accept(validCommand(SpotOrderSide.BUY), firstAcceptedAt);
        SpotSandboxOrderBookResult duplicate = boundary.accept(new SpotSandboxOrderCommand(
                "req-p16-31b",
                "idem-p16-31",
                "user-p16-31",
                "acct-p16-31",
                "BTC-USDT",
                SpotOrderSide.SELL,
                SpotOrderType.LIMIT,
                new BigDecimal("51000.00"),
                new BigDecimal("0.20"),
                SpotTimeInForce.GTC
        ), secondAcceptedAt);

        assertEquals(SpotSandboxOrderBookDecision.ACCEPTED, first.decision());
        assertEquals(SpotSandboxOrderBookDecision.DUPLICATE, duplicate.decision());
        assertEquals(first.record().sandboxOrderId(), duplicate.record().sandboxOrderId());
        assertEquals(first.record().idempotencyKey(), duplicate.record().idempotencyKey());
        assertEquals(1, boundary.openOrders("BTC-USDT").size());
    }

    /**
     * 確認 marketSymbol 查詢只回傳該 market 的 open orders。
     *
     * 這個 case 必須存在，因為後續 matching boundary 只應該看見自己的 market book。
     */
    @Test
    void openOrdersAreScopedByMarketSymbol() {
        Instant acceptedAt = Instant.parse("2026-07-12T01:20:00Z");
        boundary.accept(validCommand(SpotOrderSide.BUY), acceptedAt);
        boundary.accept(new SpotSandboxOrderCommand(
                "req-p16-33",
                "idem-p16-33",
                "user-p16-33",
                "acct-p16-33",
                "ETH-USDT",
                SpotOrderSide.SELL,
                SpotOrderType.LIMIT,
                new BigDecimal("2500.00"),
                new BigDecimal("1.00"),
                SpotTimeInForce.GTC
        ), acceptedAt);

        List<SpotSandboxOrderRecord> btcOrders = boundary.openOrders("BTC-USDT");
        List<SpotSandboxOrderRecord> ethOrders = boundary.openOrders("ETH-USDT");

        assertEquals(1, btcOrders.size());
        assertEquals(1, ethOrders.size());
        assertEquals("BTC-USDT", btcOrders.get(0).marketSymbol());
        assertEquals("ETH-USDT", ethOrders.get(0).marketSymbol());
    }

    /**
     * 確認 in-memory book insert 不代表 persisted / reserved / matched / settled / posted。
     *
     * 這個 case 必須存在，因為 P16-T03 只是 sandbox book gate，不是 production trading runtime。
     */
    @Test
    void inMemoryBookInsertDoesNotClaimDownstreamRuntimeCompletion() {
        SpotSandboxOrderBookResult result = boundary.accept(validCommand(SpotOrderSide.BUY), Instant.parse("2026-07-12T01:25:00Z"));

        assertEquals(SpotSandboxOrderBookDecision.ACCEPTED, result.decision());
        assertFalse(result.toString().contains("persisted"));
        assertFalse(result.toString().contains("reserved"));
        assertFalse(result.toString().contains("matched"));
        assertFalse(result.toString().contains("settled"));
        assertFalse(result.toString().contains("posted"));
    }

    /**
     * 確認本題 status 不會產生 FILLED / PARTIALLY_FILLED。
     *
     * 這個 case 必須存在，因為 P16-T03 不能偷偷變成 matching engine。
     */
    @Test
    void statusDoesNotProduceFilledOrPartiallyFilled() {
        SpotSandboxOrderBookResult result = boundary.accept(validCommand(SpotOrderSide.BUY), Instant.parse("2026-07-12T01:30:00Z"));

        assertEquals(SpotSandboxOrderStatus.OPEN, result.record().status());
        assertFalse(result.record().status() == SpotSandboxOrderStatus.FILLED);
        assertFalse(result.record().status() == SpotSandboxOrderStatus.PARTIALLY_FILLED);
    }

    private static SpotSandboxOrderCommand validCommand(SpotOrderSide side) {
        return new SpotSandboxOrderCommand(
                "req-p16-31",
                "idem-p16-31",
                "user-p16-31",
                "acct-p16-31",
                "BTC-USDT",
                side,
                SpotOrderType.LIMIT,
                new BigDecimal("50000.00"),
                new BigDecimal("0.10"),
                SpotTimeInForce.GTC
        );
    }
}
