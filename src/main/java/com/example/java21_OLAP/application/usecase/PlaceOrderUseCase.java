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
 * - MARKET：為了在「限價簿」中吃單，用極端價格模擬（BUY → 超高價、SELL → 超低價）
 *   （正式版會在引擎層支援 MARKET 型別；此處為簡化）
 */
@Component
public class PlaceOrderUseCase {

    private static final BigDecimal MARKET_BUY_PRICE  = new BigDecimal("1000000000");   // 很高的價格
    private static final BigDecimal MARKET_SELL_PRICE = new BigDecimal("0.00000001");   // 很低的價格

    private final OrderService orderService;

    public PlaceOrderUseCase(OrderService orderService) {
        this.orderService = orderService;
    }

    public void handle(PlaceOrderCommand cmd) {
        // 解析 symbol（簡化：假設 quote = USDT）
        String code = cmd.symbol();
        String base = code.replace("USDT", "");
        Symbol sym = new Symbol(base, "USDT", 2, 3);

        BigDecimal price;
        if (cmd.type() == OrderType.MARKET) {
            price = (cmd.side() == OrderSide.BUY) ? MARKET_BUY_PRICE : MARKET_SELL_PRICE;
        } else {
            price = cmd.price(); // LIMIT 必須有價格
            if (price == null) throw new IllegalArgumentException("LIMIT order requires price");
        }

        // 建立訂單（剩餘 qty 初始 = 下單 qty）
        Order order = new Order(
                cmd.uid(),
                sym,
                cmd.side(),
                cmd.type(),
                price,
                cmd.qty()
        );

        // 丟給 OrderService → 撮合 → 事件落庫/發布 → 倉位更新
        orderService.processOrder(order);
    }
}
