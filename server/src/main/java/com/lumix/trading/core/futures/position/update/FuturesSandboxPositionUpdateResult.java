package com.lumix.trading.core.futures.position.update;

import com.lumix.trading.core.futures.position.FuturesPosition;
import java.util.Objects;
import java.util.Optional;

/**
 * Futures sandbox position update gate 的 immutable 結果。
 *
 * 成功結果固定同時包含買方 LONG 與賣方 SHORT，拒絕結果不得攜帶任何半成品 position。
 */
public record FuturesSandboxPositionUpdateResult(
        FuturesSandboxPositionUpdateDecision decision,
        FuturesSandboxPositionUpdateReason reason,
        Optional<FuturesPosition> buyerLongPosition,
        Optional<FuturesPosition> sellerShortPosition
) {

    public static FuturesSandboxPositionUpdateResult opened(
            FuturesPosition buyerLongPosition,
            FuturesPosition sellerShortPosition
    ) {
        return new FuturesSandboxPositionUpdateResult(
                FuturesSandboxPositionUpdateDecision.OPENED_FOR_SANDBOX,
                FuturesSandboxPositionUpdateReason.OPENED_FROM_VERIFIED_FILL,
                Optional.of(buyerLongPosition),
                Optional.of(sellerShortPosition)
        );
    }

    public static FuturesSandboxPositionUpdateResult rejected(FuturesSandboxPositionUpdateReason reason) {
        return new FuturesSandboxPositionUpdateResult(
                FuturesSandboxPositionUpdateDecision.REJECTED,
                reason,
                Optional.empty(),
                Optional.empty()
        );
    }

    public FuturesSandboxPositionUpdateResult {
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(buyerLongPosition, "buyerLongPosition must not be null");
        Objects.requireNonNull(sellerShortPosition, "sellerShortPosition must not be null");

        if (decision == FuturesSandboxPositionUpdateDecision.OPENED_FOR_SANDBOX
                && (reason != FuturesSandboxPositionUpdateReason.OPENED_FROM_VERIFIED_FILL
                || buyerLongPosition.isEmpty() || sellerShortPosition.isEmpty())) {
            throw new IllegalArgumentException("opened result requires both positions and OPENED_FROM_VERIFIED_FILL");
        }
        if (decision == FuturesSandboxPositionUpdateDecision.REJECTED
                && (buyerLongPosition.isPresent() || sellerShortPosition.isPresent())) {
            throw new IllegalArgumentException("rejected result must not include positions");
        }
    }
}
