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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryMatchingEngineTest {

    private final Symbol symbol = Symbol.builder()
            .base("BTC")
            .quote("USDT")
            .priceScale(2)
            .qtyScale(3)
            .build();

    @Test
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
}
