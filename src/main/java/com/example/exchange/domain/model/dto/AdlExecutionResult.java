/*
 * 檔案用途：領域 DTO，承載 ADL forced execution 彙總結果。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * ADL forced execution result。
 *
 * @param commandId                 execution idempotency key
 * @param executed                  是否實際改動 position / ledger
 * @param reason                    execution 或拒絕原因
 * @param requestedNotional         原始 ADL plan requested notional
 * @param plannedNotional           原始 ADL plan planned notional
 * @param executedNotional          實際執行的 reduce notional
 * @param remainingNotional         plan 中尚未覆蓋的 notional
 * @param socializedLossCharged     實際從 ADL 候選獲利中扣回的金額
 * @param steps                     每筆減倉結果
 * @param executedAt                判斷或執行時間
 */
public record AdlExecutionResult(
        String commandId,
        boolean executed,
        String reason,
        BigDecimal requestedNotional,
        BigDecimal plannedNotional,
        BigDecimal executedNotional,
        BigDecimal remainingNotional,
        BigDecimal socializedLossCharged,
        List<AdlExecutionStepResult> steps,
        Instant executedAt
) {
}
