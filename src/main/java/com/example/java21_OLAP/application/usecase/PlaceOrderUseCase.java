package com.example.java21_OLAP.application.usecase;

import com.example.java21_OLAP.application.command.PlaceOrderCommand;
import com.example.java21_OLAP.application.service.OrderService;
import com.example.java21_OLAP.domain.model.OrderType;
import com.example.java21_OLAP.domain.model.OrderSide;
import com.example.java21_OLAP.domain.model.Symbol;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 下單用例
 * - 用例層做「流程與協調」，不寫技術細節（DB/Kafka 等）
 * - 真實下單會接風控、撮合、訂單簿；此處簡化示範市價成交
 */
@Component
public class PlaceOrderUseCase {

    private final OrderService orderService;

    public PlaceOrderUseCase(OrderService orderService) {
        this.orderService = orderService;
    }

    public void handle(PlaceOrderCommand cmd) {
        // 簡單的 symbol parsing（示範：預設 quote = USDT）
        String code = cmd.symbol();
        String base = code.replace("USDT", "");
        Symbol sym = new Symbol(base, "USDT", 2, 3);

        if (cmd.type() == OrderType.MARKET) {
            // 若未帶價格，示範給一個暫定價格（實務上取標記價/深度中間價）
            BigDecimal price = (cmd.price() != null) ? cmd.price() : BigDecimal.valueOf(100);
            orderService.executeMarket(cmd.uid(), sym, cmd.side(), cmd.qty(), price);
        } else {
            // LIMIT：實務應丟到訂單簿等待撮合，這裡略過
            // TODO: orderRepo.save(new Order(...))
        }
    }
}
