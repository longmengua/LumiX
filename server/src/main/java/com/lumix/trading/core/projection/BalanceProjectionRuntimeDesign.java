package com.lumix.trading.core.projection;

import java.util.List;
import java.util.Objects;

/**
 * balance projection runtime 的設計輸出。
 *
 * 這份設計只說明 read model 如何從 ledger 重建，不代表已能更新 balance_projections。
 */
public record BalanceProjectionRuntimeDesign(
        List<BalanceProjectionCapability> capabilities,
        List<String> sourceOfTruthRules,
        List<String> observabilityRules,
        List<String> noGoConditions,
        List<String> derivedBalanceFields
) {

    public BalanceProjectionRuntimeDesign {
        // 設計輸出必須可重建與可審核，不能留下可變集合參考。
        Objects.requireNonNull(capabilities, "capabilities must not be null");
        Objects.requireNonNull(sourceOfTruthRules, "sourceOfTruthRules must not be null");
        Objects.requireNonNull(observabilityRules, "observabilityRules must not be null");
        Objects.requireNonNull(noGoConditions, "noGoConditions must not be null");
        Objects.requireNonNull(derivedBalanceFields, "derivedBalanceFields must not be null");
        capabilities = List.copyOf(capabilities);
        sourceOfTruthRules = List.copyOf(sourceOfTruthRules);
        observabilityRules = List.copyOf(observabilityRules);
        noGoConditions = List.copyOf(noGoConditions);
        derivedBalanceFields = List.copyOf(derivedBalanceFields);
    }
}
