package com.lumix.trading.core.spot.orderintake;

import java.util.List;
import java.util.Objects;

/**
 * Spot sandbox order intake 的設計契約。
 *
 * 這份契約只描述 intake boundary 與驗證規則，不代表已經可以持久化或進入 matching / settlement。
 */
public record SpotSandboxOrderIntakeDesign(
        List<String> capabilities,
        List<String> intakeRules,
        List<String> validationRules,
        List<SpotSandboxOrderRejectionReason> rejectionReasons,
        List<String> noGoConditions
) {

    /**
     * 建立不可變的 intake 設計輸出。
     *
     * 這裡要先複製集合，避免後續測試或文件讀取時誤把設計資料當成可變 runtime 狀態。
     */
    public SpotSandboxOrderIntakeDesign {
        Objects.requireNonNull(capabilities, "capabilities must not be null");
        Objects.requireNonNull(intakeRules, "intakeRules must not be null");
        Objects.requireNonNull(validationRules, "validationRules must not be null");
        Objects.requireNonNull(rejectionReasons, "rejectionReasons must not be null");
        Objects.requireNonNull(noGoConditions, "noGoConditions must not be null");
        capabilities = List.copyOf(capabilities);
        intakeRules = List.copyOf(intakeRules);
        validationRules = List.copyOf(validationRules);
        rejectionReasons = List.copyOf(rejectionReasons);
        noGoConditions = List.copyOf(noGoConditions);
    }
}
