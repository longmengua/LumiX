package com.lumix.trading.core.spot.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.trading.core.spot.orderbook.InMemorySpotSandboxOrderBook;
import com.lumix.trading.core.spot.orderbook.SpotSandboxOrderRecord;
import com.lumix.trading.core.spot.orderbook.SpotSandboxOrderStatus;
import com.lumix.trading.core.spot.orderintake.SpotOrderSide;
import com.lumix.trading.core.spot.orderintake.SpotOrderType;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderCommand;
import com.lumix.trading.core.spot.orderintake.SpotTimeInForce;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Spot sandbox in-memory matching runtime 只停在 sandbox boundary，不會偷接成正式 matching engine。
 */
class SpotSandboxMatchingRuntimeBoundaryTest {

    /**
     * 確認 BUY price >= SELL price 時可以 match，且結果只會留在 memory。
     *
     * 這個 case 必須存在，因為 sandbox matching 的第一個正向路徑必須可被審核與重現。
     */
    @Test
    void matchesCrossedOrdersInMemoryOnly() {
        InMemorySpotSandboxOrderBook orderBook = new InMemorySpotSandboxOrderBook();
        SpotSandboxMatchingRuntimeBoundary boundary = new SpotSandboxMatchingRuntimeBoundary(orderBook);

        orderBook.accept(validCommand("idem-p16-50-buy", SpotOrderSide.BUY, new BigDecimal("50010.00"), new BigDecimal("0.80")), Instant.parse("2026-07-12T02:40:00Z"));
        orderBook.accept(validCommand("idem-p16-50-sell", SpotOrderSide.SELL, new BigDecimal("50000.00"), new BigDecimal("0.30")), Instant.parse("2026-07-12T02:41:00Z"));

        SpotSandboxMatchResult result = boundary.match("BTC-USDT", Instant.parse("2026-07-12T02:42:00Z"));

        assertEquals(SpotSandboxMatchDecision.MATCHED, result.decision());
        assertEquals(1, result.tradeFills().size());
        assertEquals(1, result.settlementInputs().size());
        assertEquals("BTC-USDT", result.tradeFills().get(0).marketSymbol());
        assertEquals(new BigDecimal("50010.00"), result.tradeFills().get(0).price());
        assertEquals(new BigDecimal("0.30"), result.tradeFills().get(0).quantity());
        assertEquals(new BigDecimal("15003.0000"), result.tradeFills().get(0).quoteAmount());
        assertEquals(SpotSandboxTradeFillStatus.CREATED_FOR_SANDBOX, result.tradeFills().get(0).status());
        assertEquals(SpotSandboxTradeFillStatus.SETTLEMENT_NOT_STARTED, result.settlementInputs().get(0).status());
        assertFalse(result.toString().contains("persisted"));
        assertFalse(result.toString().contains("settled"));
        assertFalse(result.toString().contains("posted"));
        assertFalse(result.toString().contains("balance"));

        SpotSandboxOrderRecord buy = orderBook.findByIdempotencyKey("idem-p16-50-buy").orElseThrow();
        SpotSandboxOrderRecord sell = orderBook.findByIdempotencyKey("idem-p16-50-sell").orElseThrow();
        assertEquals(SpotSandboxOrderStatus.PARTIALLY_FILLED, buy.status());
        assertEquals(new BigDecimal("0.50"), buy.remainingQuantity());
        assertEquals(SpotSandboxOrderStatus.FILLED, sell.status());
        assertEquals(0, sell.remainingQuantity().compareTo(BigDecimal.ZERO));
    }

    /**
     * 確認 BUY price < SELL price 時不會 match。
     *
     * 這個 case 必須存在，因為 crossed price rule 是 sandbox matching 的最基本安全邊界。
     */
    @Test
    void doesNotMatchWhenBuyPriceIsLowerThanSellPrice() {
        InMemorySpotSandboxOrderBook orderBook = new InMemorySpotSandboxOrderBook();
        SpotSandboxMatchingRuntimeBoundary boundary = new SpotSandboxMatchingRuntimeBoundary(orderBook);

        orderBook.accept(validCommand("idem-p16-51-buy", SpotOrderSide.BUY, new BigDecimal("49000.00"), new BigDecimal("0.40")), Instant.parse("2026-07-12T02:45:00Z"));
        orderBook.accept(validCommand("idem-p16-51-sell", SpotOrderSide.SELL, new BigDecimal("50000.00"), new BigDecimal("0.40")), Instant.parse("2026-07-12T02:46:00Z"));

        SpotSandboxMatchResult result = boundary.match("BTC-USDT", Instant.parse("2026-07-12T02:47:00Z"));

        assertEquals(SpotSandboxMatchDecision.NO_MATCH, result.decision());
        assertTrue(result.tradeFills().isEmpty());
        assertTrue(result.settlementInputs().isEmpty());
        assertEquals(SpotSandboxOrderStatus.OPEN, orderBook.findByIdempotencyKey("idem-p16-51-buy").orElseThrow().status());
        assertEquals(SpotSandboxOrderStatus.OPEN, orderBook.findByIdempotencyKey("idem-p16-51-sell").orElseThrow().status());
    }

    /**
     * 確認不同 marketSymbol 不得互相撮合。
     *
     * 這個 case 必須存在，因為 market partition 是 sandbox matching 的核心 boundary。
     */
    @Test
    void doesNotCrossMatchDifferentMarkets() {
        InMemorySpotSandboxOrderBook orderBook = new InMemorySpotSandboxOrderBook();
        SpotSandboxMatchingRuntimeBoundary boundary = new SpotSandboxMatchingRuntimeBoundary(orderBook);

        orderBook.accept(validCommand("idem-p16-52-buy", SpotOrderSide.BUY, new BigDecimal("51000.00"), new BigDecimal("0.25"), "BTC-USDT"), Instant.parse("2026-07-12T02:50:00Z"));
        orderBook.accept(validCommand("idem-p16-52-sell", SpotOrderSide.SELL, new BigDecimal("50000.00"), new BigDecimal("0.25"), "ETH-USDT"), Instant.parse("2026-07-12T02:51:00Z"));

        SpotSandboxMatchResult result = boundary.match("BTC-USDT", Instant.parse("2026-07-12T02:52:00Z"));

        assertEquals(SpotSandboxMatchDecision.NO_MATCH, result.decision());
        assertTrue(result.tradeFills().isEmpty());
        assertTrue(result.settlementInputs().isEmpty());
        assertEquals(SpotSandboxOrderStatus.OPEN, orderBook.findByIdempotencyKey("idem-p16-52-buy").orElseThrow().status());
        assertEquals(SpotSandboxOrderStatus.OPEN, orderBook.findByIdempotencyKey("idem-p16-52-sell").orElseThrow().status());
    }

    /**
     * 確認 BUY 高價優先、SELL 低價優先與 time priority 都有被鎖在 runtime。
     *
     * 這個 case 必須存在，因為 matching priority 一旦錯掉就會直接影響交易公平性。
     */
    @Test
    void appliesPriceAndTimePriorityDeterministically() {
        InMemorySpotSandboxOrderBook orderBook = new InMemorySpotSandboxOrderBook();
        SpotSandboxMatchingRuntimeBoundary boundary = new SpotSandboxMatchingRuntimeBoundary(orderBook);

        orderBook.accept(validCommand("idem-p16-53-buy-low", SpotOrderSide.BUY, new BigDecimal("49950.00"), new BigDecimal("0.10")), Instant.parse("2026-07-12T03:00:00Z"));
        orderBook.accept(validCommand("idem-p16-53-buy-high", SpotOrderSide.BUY, new BigDecimal("50020.00"), new BigDecimal("0.10")), Instant.parse("2026-07-12T02:59:00Z"));
        orderBook.accept(validCommand("idem-p16-53-sell-high", SpotOrderSide.SELL, new BigDecimal("50010.00"), new BigDecimal("0.10")), Instant.parse("2026-07-12T03:02:00Z"));
        orderBook.accept(validCommand("idem-p16-53-sell-low", SpotOrderSide.SELL, new BigDecimal("49990.00"), new BigDecimal("0.10")), Instant.parse("2026-07-12T03:01:00Z"));

        SpotSandboxMatchResult result = boundary.match("BTC-USDT", Instant.parse("2026-07-12T03:03:00Z"));

        assertEquals(SpotSandboxMatchDecision.MATCHED, result.decision());
        assertEquals(1, result.tradeFills().size());
        assertTrue(result.tradeFills().get(0).sandboxTradeId().startsWith("sandbox-trade-"));
        SpotSandboxOrderRecord buyHigh = orderBook.findByIdempotencyKey("idem-p16-53-buy-high").orElseThrow();
        SpotSandboxOrderRecord sellLow = orderBook.findByIdempotencyKey("idem-p16-53-sell-low").orElseThrow();
        assertEquals(buyHigh.sandboxOrderId(), result.tradeFills().get(0).buySandboxOrderId());
        assertEquals(sellLow.sandboxOrderId(), result.tradeFills().get(0).sellSandboxOrderId());
        assertEquals(new BigDecimal("50020.00"), result.tradeFills().get(0).price());
        assertEquals(new BigDecimal("0.10"), result.tradeFills().get(0).quantity());
        assertEquals(new BigDecimal("5002.0000"), result.tradeFills().get(0).quoteAmount());
    }

    /**
     * 確認 partial fill 與 full fill 的 order status 都會依 sandbox rule 更新。
     *
     * 這個 case 必須存在，因為 status 是後續 settlement / reconciliation review 的觀察點。
     */
    @Test
    void updatesStatusesForPartialAndFullFill() {
        InMemorySpotSandboxOrderBook orderBook = new InMemorySpotSandboxOrderBook();
        SpotSandboxMatchingRuntimeBoundary boundary = new SpotSandboxMatchingRuntimeBoundary(orderBook);

        orderBook.accept(validCommand("idem-p16-54-buy", SpotOrderSide.BUY, new BigDecimal("50010.00"), new BigDecimal("1.00")), Instant.parse("2026-07-12T03:10:00Z"));
        orderBook.accept(validCommand("idem-p16-54-sell", SpotOrderSide.SELL, new BigDecimal("50000.00"), new BigDecimal("1.00")), Instant.parse("2026-07-12T03:11:00Z"));

        SpotSandboxMatchResult result = boundary.match("BTC-USDT", Instant.parse("2026-07-12T03:12:00Z"));

        assertEquals(SpotSandboxMatchDecision.MATCHED, result.decision());
        assertEquals(SpotSandboxOrderStatus.FILLED, orderBook.findByIdempotencyKey("idem-p16-54-buy").orElseThrow().status());
        assertEquals(SpotSandboxOrderStatus.FILLED, orderBook.findByIdempotencyKey("idem-p16-54-sell").orElseThrow().status());
        assertEquals(0, orderBook.findByIdempotencyKey("idem-p16-54-buy").orElseThrow().remainingQuantity().compareTo(BigDecimal.ZERO));
        assertEquals(0, orderBook.findByIdempotencyKey("idem-p16-54-sell").orElseThrow().remainingQuantity().compareTo(BigDecimal.ZERO));
    }

    /**
     * 確認 partial fill 後的 order 仍留在 active queue，且同一次 match 可以繼續吃下一筆對手單。
     *
     * 這個 case 必須存在，因為 partial fill 若被移出 queue，就會讓 sandbox matching 無法續吃後續對手單。
     */
    @Test
    void partialFilledOrderRemainsActiveAndContinuesMatchingInSameRun() {
        InMemorySpotSandboxOrderBook orderBook = new InMemorySpotSandboxOrderBook();
        SpotSandboxMatchingRuntimeBoundary boundary = new SpotSandboxMatchingRuntimeBoundary(orderBook);

        orderBook.accept(validCommand("idem-p16-55-buy", SpotOrderSide.BUY, new BigDecimal("100.00"), new BigDecimal("10.00")), Instant.parse("2026-07-12T10:00:00Z"));
        orderBook.accept(validCommand("idem-p16-55-sell-a", SpotOrderSide.SELL, new BigDecimal("99.00"), new BigDecimal("4.00")), Instant.parse("2026-07-12T10:01:00Z"));
        orderBook.accept(validCommand("idem-p16-55-sell-b", SpotOrderSide.SELL, new BigDecimal("98.00"), new BigDecimal("3.00")), Instant.parse("2026-07-12T10:02:00Z"));

        SpotSandboxMatchResult result = boundary.match("BTC-USDT", Instant.parse("2026-07-12T10:03:00Z"));

        assertEquals(SpotSandboxMatchDecision.MATCHED, result.decision());
        assertEquals(2, result.tradeFills().size());
        assertEquals(SpotSandboxOrderStatus.PARTIALLY_FILLED, orderBook.findByIdempotencyKey("idem-p16-55-buy").orElseThrow().status());
        assertEquals(new BigDecimal("3.00"), orderBook.findByIdempotencyKey("idem-p16-55-buy").orElseThrow().remainingQuantity());
        assertEquals(SpotSandboxOrderStatus.FILLED, orderBook.findByIdempotencyKey("idem-p16-55-sell-a").orElseThrow().status());
        assertEquals(SpotSandboxOrderStatus.FILLED, orderBook.findByIdempotencyKey("idem-p16-55-sell-b").orElseThrow().status());
        assertEquals(0, orderBook.findByIdempotencyKey("idem-p16-55-sell-a").orElseThrow().remainingQuantity().compareTo(BigDecimal.ZERO));
        assertEquals(0, orderBook.findByIdempotencyKey("idem-p16-55-sell-b").orElseThrow().remainingQuantity().compareTo(BigDecimal.ZERO));
        assertTrue(orderBook.openOrders("BTC-USDT").stream().anyMatch(record -> record.idempotencyKey().equals("idem-p16-55-buy")));
        assertTrue(orderBook.openOrders("BTC-USDT").stream().allMatch(record -> record.remainingQuantity().compareTo(BigDecimal.ZERO) > 0));
        assertEquals(List.of(new BigDecimal("3.00"), new BigDecimal("4.00")), result.tradeFills().stream()
                .map(SpotSandboxTradeFill::quantity)
                .sorted()
                .toList());
    }

    /**
     * 確認 filled order 不得再次 match。
     *
     * 這個 case 必須存在，因為一旦 filled order 還能再次成交，就會直接破壞 sandbox matching 的安全邊界。
     */
    @Test
    void filledOrderDoesNotMatchAgain() {
        InMemorySpotSandboxOrderBook orderBook = new InMemorySpotSandboxOrderBook();
        SpotSandboxMatchingRuntimeBoundary boundary = new SpotSandboxMatchingRuntimeBoundary(orderBook);

        orderBook.accept(validCommand("idem-p16-56-buy", SpotOrderSide.BUY, new BigDecimal("100.00"), new BigDecimal("1.00")), Instant.parse("2026-07-12T11:00:00Z"));
        orderBook.accept(validCommand("idem-p16-56-sell", SpotOrderSide.SELL, new BigDecimal("99.00"), new BigDecimal("1.00")), Instant.parse("2026-07-12T11:01:00Z"));

        SpotSandboxMatchResult firstResult = boundary.match("BTC-USDT", Instant.parse("2026-07-12T11:02:00Z"));
        SpotSandboxMatchResult secondResult = boundary.match("BTC-USDT", Instant.parse("2026-07-12T11:03:00Z"));

        assertEquals(SpotSandboxMatchDecision.MATCHED, firstResult.decision());
        assertEquals(1, firstResult.tradeFills().size());
        assertEquals(SpotSandboxOrderStatus.FILLED, orderBook.findByIdempotencyKey("idem-p16-56-buy").orElseThrow().status());
        assertEquals(SpotSandboxOrderStatus.FILLED, orderBook.findByIdempotencyKey("idem-p16-56-sell").orElseThrow().status());
        assertTrue(secondResult.tradeFills().isEmpty());
        assertTrue(orderBook.openOrders("BTC-USDT").isEmpty());
    }

    private static SpotSandboxOrderCommand validCommand(
            String idempotencyKey,
            SpotOrderSide side,
            BigDecimal price,
            BigDecimal quantity
    ) {
        return validCommand(idempotencyKey, side, price, quantity, "BTC-USDT");
    }

    private static SpotSandboxOrderCommand validCommand(
            String idempotencyKey,
            SpotOrderSide side,
            BigDecimal price,
            BigDecimal quantity,
            String marketSymbol
    ) {
        return new SpotSandboxOrderCommand(
                "req-" + idempotencyKey,
                idempotencyKey,
                "user-" + idempotencyKey,
                "acct-" + idempotencyKey,
                marketSymbol,
                side,
                SpotOrderType.LIMIT,
                price,
                quantity,
                SpotTimeInForce.GTC
        );
    }
}
