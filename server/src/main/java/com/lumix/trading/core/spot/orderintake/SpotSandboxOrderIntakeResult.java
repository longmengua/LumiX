package com.lumix.trading.core.spot.orderintake;

import java.util.Objects;

/**
 * Spot sandbox order intake 的最終結果。
 *
 * accepted 只代表 sandbox command 通過 intake validation，不代表 order persisted、reserved、matched、settled 或 posted。
 */
public record SpotSandboxOrderIntakeResult(
        SpotSandboxOrderDecision decision,
        SpotSandboxOrderCommand command,
        SpotSandboxOrderRejection rejection
) {

    /**
     * 建立 accepted intake result。
     *
     * 這裡只做 domain-level accepted 標記，不代表任何 runtime 寫入已完成。
     */
    public static SpotSandboxOrderIntakeResult accepted(SpotSandboxOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return new SpotSandboxOrderIntakeResult(SpotSandboxOrderDecision.ACCEPTED, command, null);
    }

    /**
     * 建立 rejected intake result。
     *
     * 這裡只保存安全的拒絕原因，不包含任何敏感技術細節。
     */
    public static SpotSandboxOrderIntakeResult rejected(SpotSandboxOrderCommand command, SpotSandboxOrderRejection rejection) {
        Objects.requireNonNull(rejection, "rejection must not be null");
        return new SpotSandboxOrderIntakeResult(SpotSandboxOrderDecision.REJECTED, command, rejection);
    }
}
