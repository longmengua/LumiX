package com.lumix.trading.core.posting;

import java.util.List;
import java.util.Objects;

/**
 * ledger posting integration 的設計輸出。
 *
 * 這份設計只服務 phase 15 的 gate，不能被誤解為已可正式過帳。
 */
public record LedgerPostingIntegrationDesign(
        List<LedgerPostingIntegrationStep> steps,
        List<String> integrationRules,
        List<String> noGoConditions
) {

    public LedgerPostingIntegrationDesign {
        // 設計輸出必須可重建、可審核，不能留下可變集合參考。
        Objects.requireNonNull(steps, "steps must not be null");
        Objects.requireNonNull(integrationRules, "integrationRules must not be null");
        Objects.requireNonNull(noGoConditions, "noGoConditions must not be null");
        steps = List.copyOf(steps);
        integrationRules = List.copyOf(integrationRules);
        noGoConditions = List.copyOf(noGoConditions);
    }
}
