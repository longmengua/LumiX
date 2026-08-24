package com.lumix.recovery;
import java.time.Instant; import java.util.Objects;
/** incident/reconciliation/escalation evidence；不含 restore command、資料 mutation 或自動 resume。 */
public record RecoveryEvidence(String incidentReference,RecoveryReadiness readiness,Instant evaluatedAt){public RecoveryEvidence{incidentReference=Objects.requireNonNull(incidentReference,"incidentReference").trim();readiness=Objects.requireNonNull(readiness,"readiness");evaluatedAt=Objects.requireNonNull(evaluatedAt,"evaluatedAt");if(incidentReference.isEmpty())throw new IllegalArgumentException("incident reference must not be blank");}}
