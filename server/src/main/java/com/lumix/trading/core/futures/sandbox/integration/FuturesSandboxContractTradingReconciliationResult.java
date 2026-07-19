package com.lumix.trading.core.futures.sandbox.integration;

import java.util.Objects;

/**
 * P20-T03 contract-trading sandbox flow 的 immutable 重放對帳結果。
 *
 * 兩份 flow result 都被保留，讓 audit 可判讀重放預期與待驗證紀錄的差異；此型別
 * 沒有修正方法，因為 mismatch 必須交由後續人工 review，而非在 sandbox 中靜默覆寫。
 */
public record FuturesSandboxContractTradingReconciliationResult(
        FuturesSandboxContractTradingReconciliationDecision decision,
        FuturesSandboxContractTradingReconciliationReason reason,
        FuturesSandboxContractTradingFlowResult replayedFlow,
        FuturesSandboxContractTradingFlowResult recordedFlow
) {

    public FuturesSandboxContractTradingReconciliationResult {
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(replayedFlow, "replayedFlow must not be null");
        Objects.requireNonNull(recordedFlow, "recordedFlow must not be null");
        boolean matches = replayedFlow.equals(recordedFlow);
        if (decision == FuturesSandboxContractTradingReconciliationDecision.CONSISTENT
                && (reason != FuturesSandboxContractTradingReconciliationReason.REPLAY_MATCHES_RECORDED_FLOW
                || !matches)) {
            throw new IllegalArgumentException("consistent reconciliation result must contain matching flows");
        }
        if (decision == FuturesSandboxContractTradingReconciliationDecision.MISMATCH_DETECTED
                && (reason != FuturesSandboxContractTradingReconciliationReason.REPLAY_DIFFERS_FROM_RECORDED_FLOW
                || matches)) {
            throw new IllegalArgumentException("mismatch reconciliation result must contain different flows");
        }
    }

    public static FuturesSandboxContractTradingReconciliationResult consistent(
            FuturesSandboxContractTradingFlowResult flow
    ) {
        return new FuturesSandboxContractTradingReconciliationResult(
                FuturesSandboxContractTradingReconciliationDecision.CONSISTENT,
                FuturesSandboxContractTradingReconciliationReason.REPLAY_MATCHES_RECORDED_FLOW,
                flow,
                flow
        );
    }

    public static FuturesSandboxContractTradingReconciliationResult mismatch(
            FuturesSandboxContractTradingFlowResult replayedFlow,
            FuturesSandboxContractTradingFlowResult recordedFlow
    ) {
        return new FuturesSandboxContractTradingReconciliationResult(
                FuturesSandboxContractTradingReconciliationDecision.MISMATCH_DETECTED,
                FuturesSandboxContractTradingReconciliationReason.REPLAY_DIFFERS_FROM_RECORDED_FLOW,
                replayedFlow,
                recordedFlow
        );
    }
}
