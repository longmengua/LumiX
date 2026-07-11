package com.lumix.trading.core.reservation;

import java.util.List;
import java.util.Objects;

/**
 * reservation hold/release 的設計輸出。
 *
 * 這份契約只描述未來 reservation runtime 的邊界與語意，不代表可直接寫入 reservations 或 balance_projections。
 */
public record ReservationHoldReleaseDesign(
        ReservationLifecycleDecision lifecycleDecision,
        List<ReservationOperationType> operations,
        List<String> lifecycleRules,
        List<String> boundaryRules,
        List<String> idempotencyRules,
        List<String> noGoConditions
) {

    public ReservationHoldReleaseDesign {
        // 設計輸出必須可重建、可審核，不能留下可變集合參考。
        Objects.requireNonNull(lifecycleDecision, "lifecycleDecision must not be null");
        Objects.requireNonNull(operations, "operations must not be null");
        Objects.requireNonNull(lifecycleRules, "lifecycleRules must not be null");
        Objects.requireNonNull(boundaryRules, "boundaryRules must not be null");
        Objects.requireNonNull(idempotencyRules, "idempotencyRules must not be null");
        Objects.requireNonNull(noGoConditions, "noGoConditions must not be null");
        operations = List.copyOf(operations);
        lifecycleRules = List.copyOf(lifecycleRules);
        boundaryRules = List.copyOf(boundaryRules);
        idempotencyRules = List.copyOf(idempotencyRules);
        noGoConditions = List.copyOf(noGoConditions);
    }
}
