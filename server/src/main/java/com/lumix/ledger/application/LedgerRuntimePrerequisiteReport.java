package com.lumix.ledger.application;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * ledger runtime prerequisite 檢查結果。
 *
 * 這份 report 只負責描述是否已經具備啟動條件，不負責執行任何帳務動作。
 */
public record LedgerRuntimePrerequisiteReport(
        EnumSet<LedgerRuntimePrerequisite> required,
        EnumSet<LedgerRuntimePrerequisite> available,
        EnumSet<LedgerRuntimePrerequisite> missing
) {

    public LedgerRuntimePrerequisiteReport {
        // report 只做可維護的狀態描述，不能留下可變集合參考。
        required = copyOf(required, "required");
        available = copyOf(available, "available");
        missing = copyOf(missing, "missing");
    }

    /**
     * 判斷 prerequisite 是否已完整滿足。
     *
     * 如果還有任何 missing 項目，就代表 ledger runtime 仍應停留在 boundary/skeleton 階段。
     */
    public boolean isReady() {
        return missing.isEmpty();
    }

    private static EnumSet<LedgerRuntimePrerequisite> copyOf(
            Set<LedgerRuntimePrerequisite> source,
            String fieldName
    ) {
        Objects.requireNonNull(source, fieldName + " must not be null");
        if (source.isEmpty()) {
            return EnumSet.noneOf(LedgerRuntimePrerequisite.class);
        }

        return EnumSet.copyOf(source);
    }
}
