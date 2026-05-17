/*
 * 檔案用途：應用層 Command，承載 UseCase 執行所需的輸入資料。
 */
package com.example.exchange.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CancelReplaceOrderCommand(
        UUID orderId,
        long uid,
        BigDecimal price,
        BigDecimal qty,
        String clientOrderId
) {}
