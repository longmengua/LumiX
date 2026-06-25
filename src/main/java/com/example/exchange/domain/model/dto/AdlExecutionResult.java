/*
 * 檔案用途：領域 DTO，承載 ADL forced execution 彙總結果。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


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
@Data
@Builder
@Jacksonized
public class AdlExecutionResult {

    private final String commandId;

    private final boolean executed;

    private final String reason;

    private final BigDecimal requestedNotional;

    private final BigDecimal plannedNotional;

    private final BigDecimal executedNotional;

    private final BigDecimal remainingNotional;

    private final BigDecimal socializedLossCharged;

    private final List<AdlExecutionStepResult> steps;

    private final Instant executedAt;
    public AdlExecutionResult(String commandId, boolean executed, String reason, BigDecimal requestedNotional, BigDecimal plannedNotional, BigDecimal executedNotional, BigDecimal remainingNotional, BigDecimal socializedLossCharged, List<AdlExecutionStepResult> steps, Instant executedAt) {
        this.commandId = commandId;
        this.executed = executed;
        this.reason = reason;
        this.requestedNotional = requestedNotional;
        this.plannedNotional = plannedNotional;
        this.executedNotional = executedNotional;
        this.remainingNotional = remainingNotional;
        this.socializedLossCharged = socializedLossCharged;
        this.steps = steps;
        this.executedAt = executedAt;
    }

    public String commandId() {
        return commandId;
    }

    public boolean executed() {
        return executed;
    }

    public String reason() {
        return reason;
    }

    public BigDecimal requestedNotional() {
        return requestedNotional;
    }

    public BigDecimal plannedNotional() {
        return plannedNotional;
    }

    public BigDecimal executedNotional() {
        return executedNotional;
    }

    public BigDecimal remainingNotional() {
        return remainingNotional;
    }

    public BigDecimal socializedLossCharged() {
        return socializedLossCharged;
    }

    public List<AdlExecutionStepResult> steps() {
        return steps;
    }

    public Instant executedAt() {
        return executedAt;
    }
}