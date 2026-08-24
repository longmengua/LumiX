package com.lumix.deposit.observation.finality;

import com.lumix.deposit.observation.DepositChainObservation;
import java.util.Objects;
import java.util.Optional;

/**
 * 將同一 identity 的 successive finality snapshots 做安全比對。
 *
 * <p>區塊 hash 改變代表 reorg evidence；確認數下降代表 provider divergence 或 reorg 風險。兩者都不能被新的數字
 * 蓋掉，而必須保留 quarantined/orphaned 狀態並通知 network health policy。</p>
 */
public final class DepositObservationFinalityPolicy {

    public DepositObservationFinalityAssessment evaluate(
            Optional<DepositObservationFinalityState> previous,
            DepositChainObservation current,
            RequiredConfirmations requiredConfirmations
    ) {
        Optional<DepositObservationFinalityState> prior = Objects.requireNonNull(previous, "previous must not be null");
        DepositChainObservation currentObservation = Objects.requireNonNull(current, "current must not be null");
        RequiredConfirmations threshold = Objects.requireNonNull(
                requiredConfirmations, "requiredConfirmations must not be null");
        prior.ifPresent(value -> requireSameIdentity(value, currentObservation));

        if (prior.isPresent() && !prior.orElseThrow().block().equals(currentObservation.block())) {
            return assessment(currentObservation, DepositObservationLifecycle.ORPHANED, ObservationSafetyEvent.REORG_DETECTED);
        }
        if (prior.isPresent()
                && currentObservation.finality().confirmationCount() < prior.orElseThrow().confirmationCount()) {
            return assessment(currentObservation, DepositObservationLifecycle.QUARANTINED,
                    ObservationSafetyEvent.CONFIRMATION_REGRESSION);
        }
        DepositObservationLifecycle lifecycle = currentObservation.finality().confirmationCount() >= threshold.value()
                ? DepositObservationLifecycle.FINALITY_THRESHOLD_MET
                : DepositObservationLifecycle.PENDING_CONFIRMATION;
        return assessment(currentObservation, lifecycle, ObservationSafetyEvent.NONE);
    }

    private static DepositObservationFinalityAssessment assessment(
            DepositChainObservation observation,
            DepositObservationLifecycle lifecycle,
            ObservationSafetyEvent event
    ) {
        return new DepositObservationFinalityAssessment(new DepositObservationFinalityState(
                observation.identity(), observation.block(), observation.finality().confirmationCount(), lifecycle), event);
    }

    private static void requireSameIdentity(DepositObservationFinalityState previous, DepositChainObservation current) {
        if (!previous.identity().equals(current.identity())) {
            throw new IllegalArgumentException("finality comparison requires the same observation identity");
        }
    }
}
