package com.example.exchange.application.command;

import java.math.BigDecimal;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.model.enums.OrderSide;

/**
 * 下單指令（用例輸入）
 * - 用例層只接收原始請求資料，不做複雜商業邏輯
 * - 真正的驗證/風控在 UseCase/Service/Domain 內處理
 */
public record PlaceOrderCommand(
        long uid,           // 使用者 ID
        String symbol,      // 交易對（例如 "BTCUSDT"）
        OrderSide side,     // BUY / SELL
        OrderType type,     // MARKET / LIMIT
        BigDecimal price,   // 價格（市價單可為 null）
        BigDecimal qty,     // 數量
        int leverage,       // 槓桿倍數（1~125）
        String marginMode,   // "CROSS" / "ISOLATED"
        String clientOrderId,
        String timeInForce,
        boolean reduceOnly,
        boolean postOnly
) {}
