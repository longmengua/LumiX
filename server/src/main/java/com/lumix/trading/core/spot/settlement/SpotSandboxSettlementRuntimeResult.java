package com.lumix.trading.core.spot.settlement;

import java.util.Objects;

/**
 * Spot sandbox settlement runtime gate 的結果。
 *
 * 這份 result 只保存 plan / rejection / message，不代表 settlement、ledger posting 或 balance refresh 已完成。
 */
public record SpotSandboxSettlementRuntimeResult(
        SpotSandboxSettlementRuntimeDecision decision,
        SpotSandboxSettlementPlan plan,
        String rejectionReason,
        String message
) {

    /**
     * 建立 planned result。
     *
     * 這裡只表示 sandbox settlement plan 已生成，不能被誤解成正式 settlement completed。
     */
    public static SpotSandboxSettlementRuntimeResult planned(SpotSandboxSettlementPlan plan, String message) {
        return new SpotSandboxSettlementRuntimeResult(
                SpotSandboxSettlementRuntimeDecision.PLANNED,
                plan,
                null,
                message
        );
    }

    /**
     * 建立 rejected result。
     *
     * 這裡只保存安全的拒絕原因與 message，不包含任何 SQL、stack trace 或敏感資訊。
     */
    public static SpotSandboxSettlementRuntimeResult rejected(String rejectionReason, String message) {
        return new SpotSandboxSettlementRuntimeResult(
                SpotSandboxSettlementRuntimeDecision.REJECTED,
                null,
                rejectionReason,
                message
        );
    }

    /**
     * 建立不可變的 result。
     *
     * 這裡只做必要 null 檢查，避免 result 被意外建立成半成品。
     */
    public SpotSandboxSettlementRuntimeResult {
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
