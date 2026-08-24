package com.lumix.deposit.observation.finality;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 單一 network 的 fail-closed health transition policy。
 *
 * <p>halt 為 sticky state，只有具名 recovery evidence 才能恢復；此 policy 不更新 map、時鐘或 provider，故 multi-network
 * isolation 由 caller 以各 network immutable snapshot 自然維持。</p>
 */
public final class ObservationNetworkHealthPolicy {

    /**
     * 將 reorg/confirmation safety event 映射為 sticky halt。
     */
    public ObservationNetworkHealth applySafetyEvent(
            ObservationNetworkHealth current,
            ObservationSafetyEvent event,
            Instant signaledAt
    ) {
        current = Objects.requireNonNull(current, "current must not be null");
        event = Objects.requireNonNull(event, "event must not be null");
        signaledAt = requireMonotonicSignalTime(current, signaledAt);
        ObservationNetworkHealthState target = switch (event) {
            case NONE -> current.state();
            case REORG_DETECTED -> ObservationNetworkHealthState.HALTED_REORG;
            case CONFIRMATION_REGRESSION -> ObservationNetworkHealthState.HALTED_CONFIRMATION_REGRESSION;
            case CURSOR_GAP -> ObservationNetworkHealthState.HALTED_CURSOR_GAP;
            case STALE_PROVIDER_SIGNAL -> ObservationNetworkHealthState.HALTED_STALE;
        };
        return new ObservationNetworkHealth(current.network(), target, current.lastVerifiedCursor(), signaledAt);
    }

    /**
     * 驗證連續 checkpoint；高度跳過允許值時停止該 network，而非靜默前進 cursor。
     */
    public ObservationNetworkHealth applyCheckpoint(
            ObservationNetworkHealth current,
            ObservationFeedCheckpoint checkpoint,
            long maximumAllowedBlockGap
    ) {
        current = Objects.requireNonNull(current, "current must not be null");
        checkpoint = Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        if (maximumAllowedBlockGap < 0) {
            throw new IllegalArgumentException("maximum allowed block gap must not be negative");
        }
        requireSameNetwork(current, checkpoint.network());
        Instant observedAt = requireMonotonicSignalTime(current, checkpoint.observedAt());
        if (current.state() != ObservationNetworkHealthState.HEALTHY) {
            return new ObservationNetworkHealth(current.network(), current.state(), current.lastVerifiedCursor(), observedAt);
        }
        if (current.lastVerifiedCursor().isPresent()) {
            long priorHeight = current.lastVerifiedCursor().orElseThrow().block().height();
            long incomingHeight = checkpoint.cursor().block().height();
            if (incomingHeight > priorHeight + maximumAllowedBlockGap) {
                return new ObservationNetworkHealth(current.network(), ObservationNetworkHealthState.HALTED_CURSOR_GAP,
                        current.lastVerifiedCursor(), observedAt);
            }
            if (checkpoint.cursor().compareTo(current.lastVerifiedCursor().orElseThrow()) < 0) {
                return new ObservationNetworkHealth(current.network(), ObservationNetworkHealthState.HALTED_REORG,
                        current.lastVerifiedCursor(), observedAt);
            }
        }
        return new ObservationNetworkHealth(current.network(), ObservationNetworkHealthState.HEALTHY,
                java.util.Optional.of(checkpoint.cursor()), observedAt);
    }

    /**
     * caller 提供評估時刻，避免 policy 自行讀取不可重放的系統時鐘。
     */
    public ObservationNetworkHealth assessStaleness(
            ObservationNetworkHealth current,
            Instant assessedAt,
            Duration maximumSignalAge
    ) {
        current = Objects.requireNonNull(current, "current must not be null");
        assessedAt = Objects.requireNonNull(assessedAt, "assessedAt must not be null");
        maximumSignalAge = Objects.requireNonNull(maximumSignalAge, "maximumSignalAge must not be null");
        if (maximumSignalAge.isNegative() || maximumSignalAge.isZero()) {
            throw new IllegalArgumentException("maximum signal age must be positive");
        }
        if (assessedAt.isBefore(current.lastProviderSignalAt())) {
            throw new IllegalArgumentException("assessed time must not precede last provider signal time");
        }
        if (current.state() != ObservationNetworkHealthState.HEALTHY
                || !assessedAt.isAfter(current.lastProviderSignalAt().plus(maximumSignalAge))) {
            return current;
        }
        return new ObservationNetworkHealth(current.network(), ObservationNetworkHealthState.HALTED_STALE,
                current.lastVerifiedCursor(), current.lastProviderSignalAt());
    }

    /**
     * 恢復只能由顯式 evidence 提出，且不能倒退最後收到的 provider signal 時刻。
     */
    public ObservationNetworkHealth resume(ObservationNetworkHealth current, ObservationHealthRecovery recovery) {
        current = Objects.requireNonNull(current, "current must not be null");
        recovery = Objects.requireNonNull(recovery, "recovery must not be null");
        requireSameNetwork(current, recovery.network());
        if (current.state() == ObservationNetworkHealthState.HEALTHY) {
            throw new IllegalStateException("healthy network does not require recovery");
        }
        if (recovery.verifiedAt().isBefore(current.lastProviderSignalAt())) {
            throw new IllegalArgumentException("recovery time must not precede last provider signal time");
        }
        return new ObservationNetworkHealth(current.network(), ObservationNetworkHealthState.HEALTHY,
                java.util.Optional.of(recovery.verifiedCursor()), recovery.verifiedAt());
    }

    private static void requireSameNetwork(ObservationNetworkHealth current, com.lumix.deposit.address.DepositNetwork network) {
        if (!current.network().equals(network)) {
            throw new IllegalArgumentException("health transition cannot cross network boundary");
        }
    }

    private static Instant requireMonotonicSignalTime(ObservationNetworkHealth current, Instant signaledAt) {
        signaledAt = Objects.requireNonNull(signaledAt, "signaledAt must not be null");
        if (signaledAt.isBefore(current.lastProviderSignalAt())) {
            throw new IllegalArgumentException("provider signal time must not regress");
        }
        return signaledAt;
    }
}
