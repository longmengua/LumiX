/*
 * 檔案用途：應用層 Command，承載 ADL forced execution use case 輸入資料。
 */
package com.example.exchange.application.command;

import com.example.exchange.domain.model.dto.AdlDeleveragingPlan;

/**
 * ADL forced execution command。
 *
 * @param commandId execution idempotency key
 * @param plan      deterministic ADL deleveraging plan
 */
public record ExecuteAdlCommand(String commandId, AdlDeleveragingPlan plan) {
}
