package com.lumix.observability;
import java.util.Objects;
/** P31 fail-closed routing contract；critical signal 缺 runbook 視為 telemetry/operational gap。 */
public final class AlertRoutingPolicy { public AlertRoutingDecision evaluate(OperationalSignal signal,String runbookReference){signal=Objects.requireNonNull(signal,"signal");runbookReference=Objects.requireNonNull(runbookReference,"runbookReference").trim();if(signal.severity()==OperationalSignalSeverity.CRITICAL&&runbookReference.isEmpty())return AlertRoutingDecision.MISSING_TELEMETRY_REJECTED;return signal.severity()==OperationalSignalSeverity.CRITICAL?AlertRoutingDecision.ESCALATE_WITH_RUNBOOK:AlertRoutingDecision.RECORD_ONLY;} }
