package com.lumix.trading.core.spot.matching;

import java.util.List;
import java.util.Objects;

/**
 * Spot sandbox matching 的結果。
 *
 * matched / no match / rejected 只表示 sandbox matcher 的決策，不代表 settlement、ledger posting 或 balance 更新已完成。
 */
public record SpotSandboxMatchResult(
        SpotSandboxMatchDecision decision,
        List<SpotSandboxTradeFill> tradeFills,
        List<SpotSandboxSettlementInput> settlementInputs,
        SpotSandboxMatchRejectionReason rejectionReason,
        String message
) {

    /**
     * 建立 matched result。
     *
     * 這裡只表示 sandbox matching 產生 trade / fill 與 settlement input，不代表任何正式 runtime 已完成。
     */
    public static SpotSandboxMatchResult matched(
            List<SpotSandboxTradeFill> tradeFills,
            List<SpotSandboxSettlementInput> settlementInputs,
            String message
    ) {
        return new SpotSandboxMatchResult(
                SpotSandboxMatchDecision.MATCHED,
                tradeFills,
                settlementInputs,
                null,
                message
        );
    }

    /**
     * 建立 no match result。
     *
     * 這裡只表示本次 sandbox matching 沒有跨價成功，不代表 order book 或 settlement 已完成。
     */
    public static SpotSandboxMatchResult noMatch(String message) {
        return new SpotSandboxMatchResult(
                SpotSandboxMatchDecision.NO_MATCH,
                List.of(),
                List.of(),
                SpotSandboxMatchRejectionReason.NO_CROSSED_ORDERS,
                message
        );
    }

    /**
     * 建立 rejected result。
     *
     * 這裡只保存安全的拒絕原因與 message，不包含任何 SQL、stack trace 或敏感資訊。
     */
    public static SpotSandboxMatchResult rejected(SpotSandboxMatchRejectionReason reason, String message) {
        return new SpotSandboxMatchResult(
                SpotSandboxMatchDecision.REJECTED,
                List.of(),
                List.of(),
                reason,
                message
        );
    }

    /**
     * 建立不可變的 matching result。
     *
     * 這裡會複製清單，避免後續 runtime 或測試誤把結果當成可變結構。
     */
    public SpotSandboxMatchResult {
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(tradeFills, "tradeFills must not be null");
        Objects.requireNonNull(settlementInputs, "settlementInputs must not be null");
        tradeFills = List.copyOf(tradeFills);
        settlementInputs = List.copyOf(settlementInputs);
    }
}
