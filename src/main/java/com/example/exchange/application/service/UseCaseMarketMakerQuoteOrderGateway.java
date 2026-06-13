/*
 * 檔案用途：使用內部下單 use case 將做市商 quote leg 掛成 post-only LIMIT order。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.command.PlaceOrderCommand;
import com.example.exchange.application.usecase.CancelOrderUseCase;
import com.example.exchange.application.usecase.PlaceOrderUseCase;
import com.example.exchange.domain.model.dto.MarketMakerQuoteCommand;
import com.example.exchange.domain.model.dto.TopOfBook;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.service.MatchingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UseCaseMarketMakerQuoteOrderGateway implements MarketMakerQuoteOrderGateway {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final OrderRepository orderRepository;
    private final MatchingEngine matchingEngine;

    @Override
    public int cancelOpenQuoteOrders(MarketMakerQuoteCommand command) {
        String prefix = quoteClientOrderPrefix(command);
        List<Order> staleQuoteOrders = orderRepository.findOpenOrders(command.uid(), command.symbol()).stream()
                .filter(order -> order.getClientOrderId() != null)
                .filter(order -> order.getClientOrderId().startsWith(prefix))
                .toList();

        int canceled = 0;
        for (Order order : staleQuoteOrders) {
            if (cancelOrderUseCase.handle(order.getId())) {
                canceled++;
            }
        }
        return canceled;
    }

    @Override
    public boolean cancelOrder(UUID orderId) {
        return cancelOrderUseCase.handle(orderId);
    }

    @Override
    public UUID placePostOnlyLimit(MarketMakerQuoteCommand command, OrderSide side) {
        if (wouldTakeLiquidity(command, side)) {
            throw new IllegalStateException("market maker quote would take liquidity");
        }
        Order order = placeOrderUseCase.place(toPlaceOrderCommand(command, side));
        return order.getId();
    }

    private static PlaceOrderCommand toPlaceOrderCommand(MarketMakerQuoteCommand command, OrderSide side) {
        return new PlaceOrderCommand(
                command.uid(),
                command.symbol(),
                side,
                OrderType.LIMIT,
                price(command, side),
                quantity(command, side),
                1,
                "CROSS",
                clientOrderId(command, side),
                command.refId(),
                command.marketMakerId(),
                "GTC",
                false,
                true
        );
    }

    private static BigDecimal price(MarketMakerQuoteCommand command, OrderSide side) {
        return OrderSide.BUY.equals(side) ? command.bidPrice() : command.askPrice();
    }

    private boolean wouldTakeLiquidity(MarketMakerQuoteCommand command, OrderSide side) {
        Optional<TopOfBook> top = matchingEngine.top(command.symbol());
        if (top.isEmpty()) {
            return false;
        }
        BigDecimal orderPrice = price(command, side);
        if (orderPrice == null) {
            return true;
        }
        TopOfBook book = top.get();
        // Maker quotes must remain maker-only: they may improve the book, but cannot cross the visible top.
        return OrderSide.BUY.equals(side)
                ? book.getBestAsk() != null && orderPrice.compareTo(book.getBestAsk()) >= 0
                : book.getBestBid() != null && orderPrice.compareTo(book.getBestBid()) <= 0;
    }

    private static BigDecimal quantity(MarketMakerQuoteCommand command, OrderSide side) {
        return OrderSide.BUY.equals(side) ? command.bidQuantity() : command.askQuantity();
    }

    private static String clientOrderId(MarketMakerQuoteCommand command, OrderSide side) {
        String ref = command.refId() == null || command.refId().isBlank()
                ? "quote"
                : command.refId().trim();
        return quoteClientOrderPrefix(command) + ref + ":" + side.name().toLowerCase();
    }

    private static String quoteClientOrderPrefix(MarketMakerQuoteCommand command) {
        return "mmq:" + command.marketMakerId().trim() + ":";
    }
}
