package com.lumix.trading.core.futures.sandbox.contract;

import java.util.Objects;
import java.util.Optional;

/**
 * 受限 futures sandbox contract gate 的 immutable 結果。
 *
 * 成功與拒絕結果不可混入 inspection 半成品，讓後續 adapter 無法把未通過範圍檢查的輸入誤當成有效 sandbox 資料。
 */
public record FuturesSandboxContractEligibilityResult(
        FuturesSandboxContractEligibilityDecision decision,
        FuturesSandboxContractEligibilityReason reason,
        Optional<FuturesSandboxContractInspection> inspection
) {

    public static FuturesSandboxContractEligibilityResult eligible(FuturesSandboxContractInspection inspection) {
        return new FuturesSandboxContractEligibilityResult(
                FuturesSandboxContractEligibilityDecision.ELIGIBLE_FOR_SANDBOX_INSPECTION,
                FuturesSandboxContractEligibilityReason.ACCEPTED_ORDER_AND_MOCK_PRICE_WITHIN_CONTRACT,
                Optional.of(inspection)
        );
    }

    public static FuturesSandboxContractEligibilityResult rejected(FuturesSandboxContractEligibilityReason reason) {
        return new FuturesSandboxContractEligibilityResult(
                FuturesSandboxContractEligibilityDecision.REJECTED,
                reason,
                Optional.empty()
        );
    }

    public FuturesSandboxContractEligibilityResult {
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(inspection, "inspection must not be null");
        if (decision == FuturesSandboxContractEligibilityDecision.ELIGIBLE_FOR_SANDBOX_INSPECTION
                && (reason != FuturesSandboxContractEligibilityReason.ACCEPTED_ORDER_AND_MOCK_PRICE_WITHIN_CONTRACT
                || inspection.isEmpty())) {
            throw new IllegalArgumentException("eligible result requires inspection and accepted input reason");
        }
        if (decision == FuturesSandboxContractEligibilityDecision.REJECTED && inspection.isPresent()) {
            throw new IllegalArgumentException("rejected result must not include inspection");
        }
    }
}
