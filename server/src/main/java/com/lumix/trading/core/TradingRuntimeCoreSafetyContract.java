package com.lumix.trading.core;

import java.util.List;
import java.util.Objects;

/**
 * Trading Runtime Core 的安全契約輸出。
 *
 * 這份契約只用來描述 boundary 與 safety contracts，不代表已經有正式 runtime。
 */
public record TradingRuntimeCoreSafetyContract(
        List<TradingRuntimeCoreScope> scopes,
        List<String> scopeBoundaries,
        List<String> safetyContracts,
        List<String> noGoConditions,
        List<String> earliestAllowedWork
) {

    public TradingRuntimeCoreSafetyContract {
        // 契約內容必須可重建與可審核，不能留下可變集合參考。
        Objects.requireNonNull(scopes, "scopes must not be null");
        Objects.requireNonNull(scopeBoundaries, "scopeBoundaries must not be null");
        Objects.requireNonNull(safetyContracts, "safetyContracts must not be null");
        Objects.requireNonNull(noGoConditions, "noGoConditions must not be null");
        Objects.requireNonNull(earliestAllowedWork, "earliestAllowedWork must not be null");
        scopes = List.copyOf(scopes);
        scopeBoundaries = List.copyOf(scopeBoundaries);
        safetyContracts = List.copyOf(safetyContracts);
        noGoConditions = List.copyOf(noGoConditions);
        earliestAllowedWork = List.copyOf(earliestAllowedWork);
    }
}
