/*
 * 檔案用途：撮合基礎設施，提供目前的 in-memory matching engine 實作。
 */
package com.example.exchange.infra.matching;

import com.example.exchange.domain.event.TradeExecuted;
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
