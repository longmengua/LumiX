package com.lumix.ledger.application.posting;

import com.lumix.ledger.domain.LedgerInvariantViolation;
import com.lumix.ledger.persistence.LedgerAppendOnlyPersistenceMapping;

import java.util.List;
import java.util.Objects;

/**
 * ledger posting 的 application plan。
 *
 * 這份 plan 只表示 command 已通過 prereq / invariant gate，並整理出後續寫入時應使用的 mapping contract；
 * 它不等於已寫入，也不等於 balance 已更新。
 */
public record LedgerPostingPlan(
        LedgerPostingCommand command,
        LedgerAppendOnlyPersistenceMapping appendOnlyMapping,
        List<LedgerInvariantViolation> invariantViolations
) {

    public LedgerPostingPlan {
        // accepted plan 必須能被後續 review 重建，但不能帶入 mutable 參考。
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(appendOnlyMapping, "appendOnlyMapping must not be null");
        Objects.requireNonNull(invariantViolations, "invariantViolations must not be null");
        invariantViolations = List.copyOf(invariantViolations);
    }
}
