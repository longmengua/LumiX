package com.lumix.ledger.application;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 只負責 prerequisite 檢查的 ledger runtime boundary 預設實作。
 *
 * 這個實作刻意不包含任何 posting logic，避免把 skeleton 誤接成資金真相來源。
 */
public class DefaultLedgerRuntimeBoundary implements LedgerRuntimeBoundary {

    private static final EnumSet<LedgerRuntimePrerequisite> REQUIRED_PREREQUISITES = EnumSet.of(
            LedgerRuntimePrerequisite.IDENTITY_BOUNDARY,
            LedgerRuntimePrerequisite.ACCOUNT_BOUNDARY,
            LedgerRuntimePrerequisite.ASSET_BOUNDARY,
            LedgerRuntimePrerequisite.MARKET_BOUNDARY,
            LedgerRuntimePrerequisite.LEDGER_JOURNAL_SCHEMA,
            LedgerRuntimePrerequisite.LEDGER_ENTRY_SCHEMA,
            LedgerRuntimePrerequisite.BALANCE_PROJECTION_READ_MODEL,
            LedgerRuntimePrerequisite.APPEND_ONLY_GOVERNANCE
    );

    @Override
    public LedgerRuntimePrerequisiteReport verifyPrerequisites(Set<LedgerRuntimePrerequisite> availablePrerequisites) {
        // 這裡只做 boundary 門檻判斷，不進入任何資金異動流程。
        Objects.requireNonNull(availablePrerequisites, "availablePrerequisites must not be null");

        EnumSet<LedgerRuntimePrerequisite> available = copyToEnumSet(availablePrerequisites);
        EnumSet<LedgerRuntimePrerequisite> missing = EnumSet.copyOf(REQUIRED_PREREQUISITES);
        missing.removeAll(available);

        return new LedgerRuntimePrerequisiteReport(REQUIRED_PREREQUISITES, available, missing);
    }

    private static EnumSet<LedgerRuntimePrerequisite> copyToEnumSet(Set<LedgerRuntimePrerequisite> source) {
        // Set 可能從測試或上層 boundary 進來；這裡統一轉成 EnumSet，方便計算缺漏項目。
        if (source.isEmpty()) {
            return EnumSet.noneOf(LedgerRuntimePrerequisite.class);
        }

        return EnumSet.copyOf(source);
    }
}
