/*
 * 檔案用途：測試做市商 quote order gateway 保持 maker-only，不讓做市單變成吃單。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.usecase.CancelOrderUseCase;
import com.example.exchange.application.usecase.PlaceOrderUseCase;
import com.example.exchange.domain.model.dto.MarketMakerQuoteCommand;
import com.example.exchange.domain.model.dto.TopOfBook;
import com.example.exchange.domain.model.dto.Order;
import com.example.exchange.domain.model.dto.Symbol;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.service.MatchingEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCaseMarketMakerQuoteOrderGatewayTest {

    private final PlaceOrderUseCase placeOrderUseCase = mock(PlaceOrderUseCase.class);
    private final CancelOrderUseCase cancelOrderUseCase = mock(CancelOrderUseCase.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final MatchingEngine matchingEngine = mock(MatchingEngine.class);
    private final UseCaseMarketMakerQuoteOrderGateway gateway = new UseCaseMarketMakerQuoteOrderGateway(
            placeOrderUseCase,
            cancelOrderUseCase,
            orderRepository,
            matchingEngine
    );

    @Test
    @DisplayName("做市 BUY quote 若會碰到 best ask，送單前拒絕避免變成 taker")
    void rejectsBuyQuoteThatWouldTakeBestAsk() {
        when(matchingEngine.top("BTCUSDT")).thenReturn(Optional.of(top("100.00", "101.00")));

        // Flow: maker bid at best ask would immediately trade, so gateway blocks it before risk reserve/order creation.
        assertThatThrownBy(() -> gateway.placePostOnlyLimit(quote("101.00", "102.00"), OrderSide.BUY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("would take liquidity");

        verify(placeOrderUseCase, never()).place(any());
    }

    @Test
    @DisplayName("做市 SELL quote 若會碰到 best bid，送單前拒絕避免變成 taker")
    void rejectsSellQuoteThatWouldTakeBestBid() {
        when(matchingEngine.top("BTCUSDT")).thenReturn(Optional.of(top("100.00", "101.00")));

        // Flow: maker ask at best bid would immediately trade, so gateway preserves maker-only behavior.
        assertThatThrownBy(() -> gateway.placePostOnlyLimit(quote("99.00", "100.00"), OrderSide.SELL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("would take liquidity");

        verify(placeOrderUseCase, never()).place(any());
    }

    @Test
    @DisplayName("做市 quote 位於 spread 外側時仍可送出 post-only limit order")
    void placesQuoteWhenItDoesNotTakeVisibleLiquidity() {
        UUID orderId = UUID.randomUUID();
        when(matchingEngine.top("BTCUSDT")).thenReturn(Optional.of(top("100.00", "101.00")));
        when(placeOrderUseCase.place(any())).thenReturn(Order.builder()
                .id(orderId)
                .uid(90001)
                .symbol(Symbol.builder().base("BTC").quote("USDT").priceScale(2).qtyScale(3).build())
                .side(OrderSide.BUY)
                .price(new BigDecimal("100.50"))
                .qty(new BigDecimal("0.100"))
                .build());

        // Flow: bid below best ask adds liquidity and can continue through the normal order use case.
        UUID placed = gateway.placePostOnlyLimit(quote("100.50", "101.50"), OrderSide.BUY);

        assertThat(placed).isEqualTo(orderId);
        verify(placeOrderUseCase).place(any());
    }

    private static TopOfBook top(String bid, String ask) {
        return TopOfBook.builder()
                .bestBid(new BigDecimal(bid))
                .bestAsk(new BigDecimal(ask))
                .build();
    }

    private static MarketMakerQuoteCommand quote(String bidPrice, String askPrice) {
        return new MarketMakerQuoteCommand(
                "mm-alpha",
                90001,
                "BTCUSDT",
                new BigDecimal(bidPrice),
                new BigDecimal("0.100"),
                new BigDecimal(askPrice),
                new BigDecimal("0.100"),
                "quote-ref"
        );
    }
}
