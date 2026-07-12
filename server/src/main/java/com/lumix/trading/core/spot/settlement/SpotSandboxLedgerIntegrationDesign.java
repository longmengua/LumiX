package com.lumix.trading.core.spot.settlement;

import java.util.List;
import java.util.Objects;

/**
 * Spot sandbox settlement 與 ledger integration 的設計契約。
 *
 * 這份契約只描述 settlement plan 與 ledger posting controlled gate 的 integration 邊界，不代表正式 runtime 已完成。
 */
public record SpotSandboxLedgerIntegrationDesign(
        SpotSandboxLedgerIntegrationDecision runtimeStatus,
        List<SpotSandboxLedgerIntegrationStep> integrationSteps,
        List<SpotSandboxLedgerIntegrationRisk> risks,
        List<String> integrationRules,
        List<String> noGoConditions
) {

    /**
     * 建立不可變的 ledger integration 設計輸出。
     *
     * 這裡先複製集合，避免後續測試或文件讀取時誤把設計資料當成可變 runtime 狀態。
     */
    public SpotSandboxLedgerIntegrationDesign {
        Objects.requireNonNull(runtimeStatus, "runtimeStatus must not be null");
        Objects.requireNonNull(integrationSteps, "integrationSteps must not be null");
        Objects.requireNonNull(risks, "risks must not be null");
        Objects.requireNonNull(integrationRules, "integrationRules must not be null");
        Objects.requireNonNull(noGoConditions, "noGoConditions must not be null");
        integrationSteps = List.copyOf(integrationSteps);
        risks = List.copyOf(risks);
        integrationRules = List.copyOf(integrationRules);
        noGoConditions = List.copyOf(noGoConditions);
    }
}
