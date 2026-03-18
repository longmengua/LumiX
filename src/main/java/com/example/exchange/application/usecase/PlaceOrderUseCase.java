package com.example.exchange.application.usecase;

import com.example.exchange.application.command.PlaceOrderCommand;
import com.example.exchange.application.service.OrderService;
import com.example.exchange.domain.model.*;
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
    private static final String defaultQuote = "USDT";

    private final OrderService orderService;

    public PlaceOrderUseCase(OrderService orderService) {
        this.orderService = orderService;
    }

    public void handle(PlaceOrderCommand cmd) {
        String code = cmd.symbol();
        // TODO: 目前只接受 USDT quote，如果要其他的需要再調整代碼
        String base = code.replace(defaultQuote, "");

        Symbol sym = Symbol.builder()
                .base(base)
                .quote(defaultQuote)
                .priceScale(2)
                .qtyScale(3)
                .build();

        TimeInForce tif = (cmd.timeInForce() == null || cmd.timeInForce().isBlank())
                ? TimeInForce.GTC
                : TimeInForce.valueOf(cmd.timeInForce().trim().toUpperCase());

        BigDecimal price = null;
        // 不是市價單，就得有價格
        if (cmd.type() != OrderType.MARKET) {
            price = cmd.price();
            if (price == null) throw new IllegalArgumentException("LIMIT order requires price");
        }
        // 市價單不進入訂單簿，可以沒價格
        if (cmd.type() == OrderType.MARKET) {
            // todo: 市價單特殊處置
        }

        // ===== TODO: 風控 & 資金前置檢查 =====
        // TODO: 檢查交易對是否允許交易（maintenance / only-reduce 模式）
        // TODO: 檢查用戶交易許可（KYC、風險標記、黑名單、IP/國別合規）
        // TODO: 檢查下單尺寸限制（最小/最大 qty、最小名義金額、步進刻度）
        // TODO: 檢查帳戶模式/槓桿（CROSS/ISOLATED、leverage 區間）
        // TODO: 試算初始保證金 IM = notional/leverage（合約）或 = notional（現貨借貸則不同）
        // TODO: 試算 taker 可能成交手續費上限（預凍結備用；如 MTL/部分成交）
        // TODO: 檢查可用餘額 >= (IM + 預估手續費 + 其他預留)，不足則拒單
        // TODO: 凍結資金：凍結 IM 及「預估手續費上限」（返回 freezeId 以便後續釋放/調整）

        // 建立訂單（這版的 Order 將 qty 視為「剩餘數量」）
        // TODO: order.attachFreezeId(freezeId)  // 把凍結資金的憑證綁在訂單上
        // TODO: order.setLeverage(leverage)     // 記錄下單時的槓桿與保證金模式
        // TODO: order.setMarginMode(MarginMode.CROSS / ISOLATED)
        Order order = Order.builder()
                .uid(cmd.uid())
                .symbol(sym)
                .side(cmd.side())
                .type(cmd.type())
                .price(price)
                .qty(cmd.qty())
                .origQty(cmd.qty())
                .executedQty(BigDecimal.ZERO)
                .avgPrice(BigDecimal.ZERO)
                .timeInForce(tif)
                .reduceOnly(cmd.reduceOnly())
                .clientOrderId(cmd.clientOrderId())
                .build();

        // TODO: 記錄審計/上報（風控審核流水、神策/GA 埋點）

        orderService.processOrder(order);
    }
}
