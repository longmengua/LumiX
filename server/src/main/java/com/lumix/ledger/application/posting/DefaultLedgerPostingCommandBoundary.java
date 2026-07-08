package com.lumix.ledger.application.posting;

import com.lumix.ledger.application.LedgerRuntimeBoundary;
import com.lumix.ledger.application.LedgerRuntimePrerequisite;
import com.lumix.ledger.application.LedgerRuntimePrerequisiteReport;
import com.lumix.ledger.domain.LedgerInvariantPolicy;
import com.lumix.ledger.domain.LedgerInvariantViolation;
import com.lumix.ledger.persistence.LedgerAppendOnlyPersistencePort;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 預設的 ledger posting command boundary。
 *
 * 這個實作只產生 plan，不會寫資料庫，不會更新 balance，也不會觸發任何 side effect。
 */
public class DefaultLedgerPostingCommandBoundary implements LedgerPostingCommandBoundary {

    private final LedgerRuntimeBoundary runtimeBoundary;
    private final LedgerInvariantPolicy invariantPolicy;
    private final LedgerAppendOnlyPersistencePort persistencePort;

    public DefaultLedgerPostingCommandBoundary(
            LedgerRuntimeBoundary runtimeBoundary,
            LedgerInvariantPolicy invariantPolicy,
            LedgerAppendOnlyPersistencePort persistencePort
    ) {
        // command boundary 的依賴只限於 gate 與 mapping contract，不能接 repository 或 database client。
        this.runtimeBoundary = Objects.requireNonNull(runtimeBoundary, "runtimeBoundary must not be null");
        this.invariantPolicy = Objects.requireNonNull(invariantPolicy, "invariantPolicy must not be null");
        this.persistencePort = Objects.requireNonNull(persistencePort, "persistencePort must not be null");
    }

    @Override
    public LedgerPostingCommandResult evaluate(LedgerPostingCommand command, Set<LedgerRuntimePrerequisite> availablePrerequisites) {
        // 先驗證 command 本身，再走 prerequisite 與 invariant gate，避免把垃圾請求往下游推。
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(availablePrerequisites, "availablePrerequisites must not be null");

        LedgerRuntimePrerequisiteReport prerequisiteReport = runtimeBoundary.verifyPrerequisites(availablePrerequisites);
        if (!prerequisiteReport.isReady()) {
            return LedgerPostingCommandResult.rejected(new LedgerPostingRejection(
                    "PREREQUISITE_GATE_FAILED",
                    "Ledger runtime prerequisites are not ready.",
                    prerequisiteReport.missing().stream().map(LedgerRuntimePrerequisite::name).toList()
            ));
        }

        List<LedgerInvariantViolation> violations = invariantPolicy.validate(command.journalDraft());
        if (!violations.isEmpty()) {
            return LedgerPostingCommandResult.rejected(new LedgerPostingRejection(
                    "JOURNAL_INVARIANT_FAILED",
                    "Ledger journal draft failed invariant validation.",
                    violations.stream().map(LedgerInvariantViolation::ruleCode).toList()
            ));
        }

        LedgerPostingPlan plan = new LedgerPostingPlan(
                command,
                persistencePort.describeAppendOnlyMapping(command.journalDraft(), command.submittedAt()),
                violations
        );
        return LedgerPostingCommandResult.accepted(plan);
    }
}
