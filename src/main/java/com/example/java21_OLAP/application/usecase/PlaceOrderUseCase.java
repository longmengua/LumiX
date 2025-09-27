package com.example.java21_OLAP.application.usecase;

import com.example.java21_OLAP.application.command.PlaceOrderCommand;
import com.example.java21_OLAP.application.service.OrderService;
import com.example.java21_OLAP.domain.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 下單用例（接入撮合引擎）
 *
 * - LIMIT：使用者提供的 price/qty
 * - MARKET：為了在限價簿中吃單，用極端價格模擬（BUY → 超高價、SELL → 超低價）
 */
@Component
public class PlaceOrderUseCase {

    private static final BigDecimal MARKET_BUY_PRICE  = new BigDecimal("1000000000");   // 超高價
    private static final BigDecimal MARKET_SELL_PRICE = new BigDecimal("0.00000001");   // 超低價

    private final OrderService orderService;

    public PlaceOrderUseCase(OrderService orderService) {
        this.orderService = orderService;
    }

    public void handle(PlaceOrderCommand cmd) {
        // 解析 symbol（簡化：假設 quote=USDT）
        String code = cmd.symbol();
        String base = code.replace("USDT", "");

        // ✅ 使用 Builder 產生 Symbol（因為 Symbol 改成 @Builder/@Jacksonized）
        Symbol sym = Symbol.builder()
                .base(base)
                .quote("USDT")
                .priceScale(2)
                .qtyScale(3)
                .build();

        BigDecimal price;
        if (cmd.type() == OrderType.MARKET) {
            price = (cmd.side() == OrderSide.BUY) ? MARKET_BUY_PRICE : MARKET_SELL_PRICE;
        } else {
            price = cmd.price();
            if (price == null) throw new IllegalArgumentException("LIMIT order requires price");
        }

        // 建立訂單（這版的 Order 將 qty 視為「剩餘數量」）
        Order order = Order.builder()
                .uid(cmd.uid())
                .symbol(sym)
                .side(cmd.side())
                .type(cmd.type())
                .price(price)
                .qty(cmd.qty())
                .build();

        orderService.processOrder(order);
    }
}
