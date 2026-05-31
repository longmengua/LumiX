/*
 * 檔案用途：使用內部下單 use case 將做市商 quote leg 掛成 post-only LIMIT order。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.command.PlaceOrderCommand;
import com.example.exchange.application.usecase.PlaceOrderUseCase;
import com.example.exchange.domain.model.dto.MarketMakerQuoteCommand;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UseCaseMarketMakerQuoteOrderGateway implements MarketMakerQuoteOrderGateway {

    private final PlaceOrderUseCase placeOrderUseCase;

    @Override
    public UUID placePostOnlyLimit(MarketMakerQuoteCommand command, OrderSide side) {
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
                "GTC",
                false,
                true
        );
    }

    private static BigDecimal price(MarketMakerQuoteCommand command, OrderSide side) {
        return OrderSide.BUY.equals(side) ? command.bidPrice() : command.askPrice();
    }

    private static BigDecimal quantity(MarketMakerQuoteCommand command, OrderSide side) {
        return OrderSide.BUY.equals(side) ? command.bidQuantity() : command.askQuantity();
    }

    private static String clientOrderId(MarketMakerQuoteCommand command, OrderSide side) {
        String ref = command.refId() == null || command.refId().isBlank()
                ? "quote"
                : command.refId().trim();
        return "mmq:" + command.marketMakerId().trim() + ":" + ref + ":" + side.name().toLowerCase();
    }
}
