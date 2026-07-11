package com.lumix.trading.core.spot.matching;

import java.util.List;
import java.util.Objects;

/**
 * Spot sandbox matching 的設計契約。
 *
 * 這份契約只描述 sandbox matching 的規則與 boundary，不代表正式 trade / fill / settlement runtime 已完成。
 */
public record SpotSandboxMatchingDesign(
        SpotSandboxMatchingDecision runtimeStatus,
        List<SpotSandboxMatchingCapability> capabilities,
        List<SpotSandboxTradePriceRule> tradePriceRules,
        List<String> matchingRules,
        List<String> noGoConditions
) {

    /**
     * 建立不可變的 matching 設計輸出。
     *
     * 這裡先複製集合，避免後續測試或文件讀取時誤把設計資料當成可變 runtime 狀態。
     */
    public SpotSandboxMatchingDesign {
        Objects.requireNonNull(runtimeStatus, "runtimeStatus must not be null");
        Objects.requireNonNull(capabilities, "capabilities must not be null");
        Objects.requireNonNull(tradePriceRules, "tradePriceRules must not be null");
        Objects.requireNonNull(matchingRules, "matchingRules must not be null");
        Objects.requireNonNull(noGoConditions, "noGoConditions must not be null");
        capabilities = List.copyOf(capabilities);
        tradePriceRules = List.copyOf(tradePriceRules);
        matchingRules = List.copyOf(matchingRules);
        noGoConditions = List.copyOf(noGoConditions);
    }
}
