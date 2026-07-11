package com.lumix.trading.core.spot;

import java.util.List;
import java.util.Objects;

/**
 * Spot sandbox 的設計契約。
 *
 * 這份契約只描述 sandbox-only 的 scope 與 runtime boundaries，不代表正式 order / matching / settlement 可以執行。
 */
public record SpotSandboxBoundaryDesign(
        SpotSandboxRuntimeStatus runtimeStatus,
        List<SpotSandboxCapability> capabilities,
        List<String> sandboxRules,
        List<String> boundaryRules,
        List<String> noGoConditions
) {

    /**
     * 建立不可變的 spot sandbox 設計輸出。
     *
     * 這裡要先複製集合，避免後續測試或文件讀取時誤把設計資料當成可變 runtime 狀態。
     */
    public SpotSandboxBoundaryDesign {
        Objects.requireNonNull(runtimeStatus, "runtimeStatus must not be null");
        Objects.requireNonNull(capabilities, "capabilities must not be null");
        Objects.requireNonNull(sandboxRules, "sandboxRules must not be null");
        Objects.requireNonNull(boundaryRules, "boundaryRules must not be null");
        Objects.requireNonNull(noGoConditions, "noGoConditions must not be null");
        capabilities = List.copyOf(capabilities);
        sandboxRules = List.copyOf(sandboxRules);
        boundaryRules = List.copyOf(boundaryRules);
        noGoConditions = List.copyOf(noGoConditions);
    }
}
