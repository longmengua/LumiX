package com.lumix.deposit.credit.correction;

import com.lumix.deposit.observation.finality.DepositObservationLifecycle;
import java.util.Objects;
import java.util.Optional;

/**
 * P23-T03 的 fail-closed reorg correction decision policy。
 *
 * <p>只有 identity 一致且確定 ORPHANED 的 evidence 可進入 freeze/reversal 分支。任何未知 append state、identity 不符或
 * 非 reorg finality 都必須人工升級，避免隱性 delete/overwrite 或錯誤 reversal。</p>
 */
public final class DepositCreditReorgCorrectionPolicy {

    public DepositCreditReorgCorrectionPlan evaluate(DepositCreditReorgCorrectionRequest request) {
        request = Objects.requireNonNull(request, "request must not be null");
        if (!request.originalDecision().candidate().observationEvidence().observation().identity()
                .equals(request.reorgState().identity())) {
            return plan(DepositCreditReorgDecision.ESCALATE_HUMAN, request, Optional.empty(),
                    "reorg evidence identity does not match original credit candidate");
        }
        if (request.reorgState().lifecycle() != DepositObservationLifecycle.ORPHANED) {
            return plan(DepositCreditReorgDecision.ESCALATE_HUMAN, request, Optional.empty(),
                    "correction requires explicit orphaned reorg evidence");
        }
        return switch (request.appendState()) {
            case NOT_APPENDED -> plan(DepositCreditReorgDecision.FREEZE_PENDING_CREDIT, request, Optional.empty(),
                    "orphaned observation has no confirmed credit append; freeze future handoff");
            case CREDIT_APPEND_CONFIRMED -> request.correctionOrder().isPresent()
                    ? plan(DepositCreditReorgDecision.APPEND_ONLY_REVERSAL_REQUIRED, request, request.correctionOrder(),
                    "confirmed credit append requires a later append-only reversal")
                    : plan(DepositCreditReorgDecision.ESCALATE_HUMAN, request, Optional.empty(),
                    "confirmed credit append lacks reversal ordering evidence");
            case UNKNOWN -> plan(DepositCreditReorgDecision.ESCALATE_HUMAN, request, Optional.empty(),
                    "original credit append state is unknown");
        };
    }

    private static DepositCreditReorgCorrectionPlan plan(
            DepositCreditReorgDecision decision,
            DepositCreditReorgCorrectionRequest request,
            Optional<AppendOnlyCorrectionOrder> order,
            String reason
    ) {
        return new DepositCreditReorgCorrectionPlan(decision, request.originalDecision(), order, reason);
    }
}
