package com.lumix.operations;
import java.util.List; import java.util.Objects;
/** P35 將 ownership/runbook 與所有 business/compliance/support gap 明確交給人類 go/no-go。 */
public final class OperationalReadinessPolicy { public OperationalReadinessDecision evaluate(OperationalOwnershipEvidence ownership,List<OperationalGap> gaps){Objects.requireNonNull(ownership,"ownership");gaps=List.copyOf(Objects.requireNonNull(gaps,"gaps"));return gaps.stream().allMatch(gap->gap==OperationalGap.NONE)?OperationalReadinessDecision.READY_FOR_HUMAN_GO_NO_GO:OperationalReadinessDecision.OPEN_GAP_REJECTED;} }
