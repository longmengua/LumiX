package com.lumix.trading.core.futures.order;

import java.util.Objects;
import java.util.Optional;

/**
 * Futures sandbox order placement 的最終結果。
 *
 * accepted 只代表 order 通過 sandbox placement gate，不代表已送入 order book、已 matched、已 reserve margin、
 * 已建立 position 或已 settlement。
 */
public record FuturesOrderPlacementResult(
        FuturesOrderStatus status,
        FuturesOrderPlacementReason reason,
        Optional<FuturesSandboxOrder> acceptedOrder
) {

    /**
     * 建立 accepted placement result。
     *
     * 這裡只建立 immutable accepted snapshot，不產生任何 runtime side effect。
     */
    public static FuturesOrderPlacementResult accepted(FuturesSandboxOrder acceptedOrder) {
        return new FuturesOrderPlacementResult(
                FuturesOrderStatus.ACCEPTED_FOR_SANDBOX,
                FuturesOrderPlacementReason.SANDBOX_ORDER_ACCEPTED,
                Optional.of(acceptedOrder)
        );
    }

    /**
     * 建立 rejected placement result。
     *
     * 這裡只返回安全的業務拒絕原因，不保留任何 fake accepted order。
     */
    public static FuturesOrderPlacementResult rejected(FuturesOrderPlacementReason reason) {
        return new FuturesOrderPlacementResult(
                FuturesOrderStatus.REJECTED,
                reason,
                Optional.empty()
        );
    }

    public FuturesOrderPlacementResult {
        // placement result 不允許 decision / reason / order snapshot 彼此矛盾，避免把 accepted / rejected 語意混成半成品。
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(acceptedOrder, "acceptedOrder must not be null");

        if (status == FuturesOrderStatus.ACCEPTED_FOR_SANDBOX) {
            if (reason != FuturesOrderPlacementReason.SANDBOX_ORDER_ACCEPTED) {
                throw new IllegalArgumentException("ACCEPTED_FOR_SANDBOX status must use SANDBOX_ORDER_ACCEPTED reason");
            }
            if (acceptedOrder.isEmpty()) {
                throw new IllegalArgumentException("ACCEPTED_FOR_SANDBOX result must include acceptedOrder");
            }
        } else {
            if (reason == FuturesOrderPlacementReason.SANDBOX_ORDER_ACCEPTED) {
                throw new IllegalArgumentException("REJECTED result must not use SANDBOX_ORDER_ACCEPTED reason");
            }
            if (acceptedOrder.isPresent()) {
                throw new IllegalArgumentException("REJECTED result must not include acceptedOrder");
            }
        }
    }
}
