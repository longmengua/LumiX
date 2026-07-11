package com.lumix.trading.core.reconciliation;

import java.util.List;
import java.util.Objects;

/**
 * reconciliation 的設計輸出。
 *
 * 這份契約只描述未來對帳流程的邊界與訊號，不代表可直接修正資料或寫回任何 runtime 表。
 */
public record ReconciliationDesign(
        ReconciliationDesignDecision decision,
        List<ReconciliationSignalType> signalTypes,
        List<String> comparisonRules,
        List<String> mismatchRules,
        List<String> noGoConditions
) {

    public ReconciliationDesign {
        // 設計輸出必須可重建、可審核，不能留下可變集合參考。
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(signalTypes, "signalTypes must not be null");
        Objects.requireNonNull(comparisonRules, "comparisonRules must not be null");
        Objects.requireNonNull(mismatchRules, "mismatchRules must not be null");
        Objects.requireNonNull(noGoConditions, "noGoConditions must not be null");
        signalTypes = List.copyOf(signalTypes);
        comparisonRules = List.copyOf(comparisonRules);
        mismatchRules = List.copyOf(mismatchRules);
        noGoConditions = List.copyOf(noGoConditions);
    }
}
