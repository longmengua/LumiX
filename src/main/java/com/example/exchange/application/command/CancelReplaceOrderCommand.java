/*
 * 檔案用途：應用層 Command，承載 UseCase 執行所需的輸入資料。
 */
package com.example.exchange.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cancel-replace 命令。
 *
 * @param orderId 要取消的原訂單
 * @param uid 訂單擁有者，用於防止越權操作
 * @param price replacement price；null 代表沿用原價格
 * @param qty replacement quantity；null 代表沿用原剩餘數量
 * @param clientOrderId replacement client order id；null 代表沿用原值
 */
public record CancelReplaceOrderCommand(
        UUID orderId,
        long uid,
        BigDecimal price,
        BigDecimal qty,
        String clientOrderId
) {}
