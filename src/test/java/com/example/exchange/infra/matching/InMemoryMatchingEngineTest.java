/*
 * 檔案用途：撮合基礎設施，提供目前的 in-memory matching engine 實作。
 */
package com.example.exchange.infra.matching;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.MatchingEngineSnapshot;
import com.example.exchange.domain.model.dto.MatchingResult;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.model.enums.TimeInForce;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試 in-memory matching engine 的核心撮合規則。
 *
 * <p>覆蓋價格時間優先、post-only、自成交防護、FOK/IOC、市價單流動性不足等
 * exchange matching semantics。</p>
 */
class InMemoryMatchingEngineTest {

    private final Symbol symbol = Symbol.builder()
            .base("BTC")
            .quote("USDT")
            .priceScale(2)
            .qtyScale(3)
            .build();

    @Test
    @DisplayName("同價位掛單依 FIFO 順序成交")
    /**
     * 流程：先送兩筆同價賣單進 book -> 再送可全吃的買單 -> 驗證 maker 成交順序符合 FIFO。
     */
    void matchesSamePriceByFifoOrderId() {
        InMemoryMatchingEngine engine = new InMemoryMatchingEngine();
        Order firstAsk = limit(1, OrderSide.SELL, "100", "1");
        Order secondAsk = limit(2, OrderSide.SELL, "100", "1");
        engine.submit(firstAsk);
        engine.submit(secondAsk);

        MatchingResult result = engine.submit(limit(3, OrderSide.BUY, "100", "2"));

        List<UUID> makerOrderIds = result.getTrades().stream()
                .filter(TradeExecuted::maker)
                .map(TradeExecuted::orderId)
                .toList();
        assertThat(makerOrderIds).containsExactly(firstAsk.getId(), secondAsk.getId());
        assertThat(firstAsk.getStatus()).isEqualTo(Order.Status.FILLED);
        assertThat(secondAsk.getStatus()).isEqualTo(Order.Status.FILLED);
        engine.shutdown();
    }

    @Test
    @DisplayName("post-only 訂單若會吃單就拒絕")
    /**
     * 流程：先放一筆可成交賣單 -> 送 post-only 買單 -> 驗證不成交且 incoming order 被拒絕。
     */
    void postOnlyRejectsOrderThatWouldTakeLiquidity() {
        InMemoryMatchingEngine engine = new InMemoryMatchingEngine();
        engine.submit(limit(1, OrderSide.SELL, "100", "1"));

        Order postOnlyBuy = limit(2, OrderSide.BUY, "100", "1");
        postOnlyBuy.setPostOnly(true);
        MatchingResult result = engine.submit(postOnlyBuy);

        assertThat(result.getTrades()).isEmpty();
        assertThat(postOnlyBuy.getStatus()).isEqualTo(Order.Status.REJECTED);
        assertThat(postOnlyBuy.getRejectCode()).isEqualTo("POST_ONLY_WOULD_TAKE");
        engine.shutdown();
    }

    @Test
    @DisplayName("同 uid 自成交會拒絕 incoming order")
    /**
     * 流程：同 uid 先掛賣 -> 再送買單會與自己成交 -> 驗證 self-match prevention 拒絕 incoming。
     */
    void selfMatchPreventionRejectsIncomingOrder() {
        InMemoryMatchingEngine engine = new InMemoryMatchingEngine();
        engine.submit(limit(7, OrderSide.SELL, "100", "1"));

        Order selfBuy = limit(7, OrderSide.BUY, "100", "1");
        MatchingResult result = engine.submit(selfBuy);

        assertThat(result.getTrades()).isEmpty();
        assertThat(selfBuy.getStatus()).isEqualTo(Order.Status.REJECTED);
        assertThat(selfBuy.getRejectCode()).isEqualTo("SELF_MATCH_PREVENTED");
        engine.shutdown();
    }

    @Test
    @DisplayName("FOK 無法完全成交時失效並帶明確原因")
    /**
     * 流程：book 只有 1 單位流動性 -> FOK 買 2 單位 -> 驗證未全成時整筆過期且無成交。
     */
    void fokExpiresWithExplicitReasonWhenNotFullyFillable() {
        InMemoryMatchingEngine engine = new InMemoryMatchingEngine();
        engine.submit(limit(1, OrderSide.SELL, "100", "1"));

        Order fokBuy = limit(2, OrderSide.BUY, "100", "2");
        fokBuy.setTimeInForce(TimeInForce.FOK);
        MatchingResult result = engine.submit(fokBuy);

        assertThat(result.getTrades()).isEmpty();
        assertThat(fokBuy.getStatus()).isEqualTo(Order.Status.EXPIRED);
        assertThat(fokBuy.getRejectCode()).isEqualTo("FOK_NOT_FULLY_FILLABLE");
        engine.shutdown();
    }

    @Test
    @DisplayName("IOC 完全未成交時失效並帶明確原因")
    /**
     * 流程：book 最佳賣價高於 IOC 買價 -> 送 IOC -> 驗證未成交即過期並保留拒絕原因。
     */
    void iocExpiresWithExplicitReasonWhenUnfilled() {
        InMemoryMatchingEngine engine = new InMemoryMatchingEngine();
        engine.submit(limit(1, OrderSide.SELL, "100", "1"));

        Order iocBuy = limit(2, OrderSide.BUY, "99", "1");
        iocBuy.setTimeInForce(TimeInForce.IOC);
        MatchingResult result = engine.submit(iocBuy);

        assertThat(result.getTrades()).isEmpty();
        assertThat(iocBuy.getStatus()).isEqualTo(Order.Status.EXPIRED);
        assertThat(iocBuy.getRejectCode()).isEqualTo("IOC_NOT_FILLED");
        engine.shutdown();
    }

    @Test
    @DisplayName("市價單流動性不足時部分成交後失效")
    /**
     * 流程：book 只有 1 單位賣單 -> 市價買 2 單位 -> 驗證先吃可用流動性，再因不足而過期。
     */
    void marketOrderExpiresWithExplicitReasonWhenLiquidityIsInsufficient() {
        InMemoryMatchingEngine engine = new InMemoryMatchingEngine();
        engine.submit(limit(1, OrderSide.SELL, "100", "1"));

        Order marketBuy = market(2, OrderSide.BUY, "2");
        MatchingResult result = engine.submit(marketBuy);

        assertThat(result.getTrades()).hasSize(2);
        assertThat(marketBuy.getStatus()).isEqualTo(Order.Status.EXPIRED);
        assertThat(marketBuy.getQty()).isEqualByComparingTo("1");
        assertThat(marketBuy.getRejectCode()).isEqualTo("MARKET_LIQUIDITY_INSUFFICIENT");
        engine.shutdown();
    }

    @Test
    @DisplayName("撮合快照可還原訂單簿 FIFO 與 match sequence")
    /**
     * 流程：先產生一筆成交推進 match sequence -> 匯出兩筆同價賣單快照 -> 新 engine 還原後成交。
     */
    void snapshotRestorePreservesFifoAndMatchSequence() {
        InMemoryMatchingEngine engine = new InMemoryMatchingEngine();
        engine.submit(limit(1, OrderSide.SELL, "99", "1"));
        engine.submit(limit(2, OrderSide.BUY, "99", "1"));
        Order firstAsk = limit(3, OrderSide.SELL, "100", "1");
        Order secondAsk = limit(4, OrderSide.SELL, "100", "1");
        engine.submit(firstAsk);
        engine.submit(secondAsk);

        MatchingEngineSnapshot snapshot = engine.exportSnapshot("btcusdt");
        InMemoryMatchingEngine restored = new InMemoryMatchingEngine();
        restored.restoreSnapshot(snapshot);

        MatchingResult result = restored.submit(limit(5, OrderSide.BUY, "100", "2"));

        List<UUID> makerOrderIds = result.getTrades().stream()
                .filter(TradeExecuted::maker)
                .map(TradeExecuted::orderId)
                .toList();
        assertThat(makerOrderIds).containsExactly(firstAsk.getId(), secondAsk.getId());
        assertThat(result.getTrades()).extracting(TradeExecuted::matchId)
                .contains("BTCUSDT-2", "BTCUSDT-3");
        engine.shutdown();
        restored.shutdown();
    }

    @Test
    @DisplayName("可從 snapshot checkpoint 後的 command log replay 撮合狀態")
    /**
     * 流程：先匯出含 command offset 的 snapshot -> 原 engine 繼續處理新掛單與成交 ->
     * 新 engine 用 snapshot + 全量 command log replay -> 驗證 book 與 match sequence 延續。
     */
    void replayFromSnapshotCheckpointRebuildsBookAndMatchSequence() {
        InMemoryMatchingEngine engine = new InMemoryMatchingEngine();
        engine.submit(limit(1, OrderSide.SELL, "100", "1"));
        engine.submit(limit(2, OrderSide.SELL, "101", "1"));
        MatchingEngineSnapshot snapshot = engine.exportSnapshot("BTCUSDT");

        engine.submit(limit(3, OrderSide.SELL, "102", "1"));
        engine.submit(limit(4, OrderSide.BUY, "102", "3"));

        InMemoryMatchingEngine restored = new InMemoryMatchingEngine();
        restored.replay(snapshot, engine.commandLog("BTCUSDT"));

        assertThat(restored.snapshot("BTCUSDT", 10).asks()).isEmpty();
        MatchingResult result = restored.submit(limit(5, OrderSide.SELL, "103", "1"));
        result = restored.submit(limit(6, OrderSide.BUY, "103", "1"));

        assertThat(result.getTrades()).extracting(TradeExecuted::matchId)
                .contains("BTCUSDT-4");
        assertThat(restored.snapshot("BTCUSDT", 10).asks()).isEmpty();
        engine.shutdown();
        restored.shutdown();
    }

    /**
     * 建立 LIMIT 測試訂單，統一 symbol、price、qty 與 origQty，讓各案例只聚焦撮合規則。
     */
    private Order limit(long uid, OrderSide side, String price, String qty) {
        return Order.builder()
                .uid(uid)
                .symbol(symbol)
                .side(side)
                .type(OrderType.LIMIT)
                .price(new BigDecimal(price))
                .qty(new BigDecimal(qty))
                .origQty(new BigDecimal(qty))
                .build();
    }

    /**
     * 建立 MARKET 測試訂單；市價單沒有 price，專門用來驗證流動性不足與剩餘量處理。
     */
    private Order market(long uid, OrderSide side, String qty) {
        return Order.builder()
                .uid(uid)
                .symbol(symbol)
                .side(side)
                .type(OrderType.MARKET)
                .qty(new BigDecimal(qty))
                .origQty(new BigDecimal(qty))
                .build();
    }
}
