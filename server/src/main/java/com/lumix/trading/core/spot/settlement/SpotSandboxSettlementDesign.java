package com.lumix.trading.core.spot.settlement;

import java.util.List;
import java.util.Objects;

/**
 * Spot sandbox settlement 的設計契約。
 *
 * 這份契約只描述 settlement boundary 與 runtime 順序，不代表正式 settlement runtime、ledger posting 或 balance refresh 已完成。
 */
public record SpotSandboxSettlementDesign(
        SpotSandboxSettlementDecision runtimeStatus,
        List<SpotSandboxSettlementCapability> capabilities,
        List<SpotSandboxSettlementStep> settlementSteps,
        List<String> settlementRules,
        List<String> noGoConditions
) {

    /**
     * 建立不可變的 settlement 設計輸出。
     *
     * 這裡先複製集合，避免後續測試或文件讀取時誤把設計資料當成可變 runtime 狀態。
     */
    public SpotSandboxSettlementDesign {
        Objects.requireNonNull(runtimeStatus, "runtimeStatus must not be null");
        Objects.requireNonNull(capabilities, "capabilities must not be null");
        Objects.requireNonNull(settlementSteps, "settlementSteps must not be null");
        Objects.requireNonNull(settlementRules, "settlementRules must not be null");
        Objects.requireNonNull(noGoConditions, "noGoConditions must not be null");
        capabilities = List.copyOf(capabilities);
        settlementSteps = List.copyOf(settlementSteps);
        settlementRules = List.copyOf(settlementRules);
        noGoConditions = List.copyOf(noGoConditions);
    }
}
