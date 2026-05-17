/*
 * 檔案用途：應用層 Command，承載 UseCase 執行所需的輸入資料。
 */
package com.example.exchange.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 改單命令。
 *
 * @param orderId 要修改的原訂單
 * @param uid 訂單擁有者，用於防止越權改單
 * @param price 新價格；null 代表沿用原價格
 * @param qty 新剩餘數量；null 代表沿用原剩餘數量
 * @param clientOrderId 新 client order id；null 代表沿用原值
 */
public record AmendOrderCommand(
        UUID orderId,
        long uid,
        BigDecimal price,
        BigDecimal qty,
        String clientOrderId
) {}
