package com.lumix.performance;
import java.time.Instant; import java.util.Objects;
/** 壓測/故障演練的唯讀結果 evidence；不會操作系統、provider 或網路。 */
public record PerformanceObservation(String scenarioReference,long latencyMillis,long errorCount,boolean integrityVerified,Instant observedAt){public PerformanceObservation{scenarioReference=Objects.requireNonNull(scenarioReference,"scenarioReference").trim();observedAt=Objects.requireNonNull(observedAt,"observedAt");if(scenarioReference.isEmpty()||latencyMillis<0||errorCount<0)throw new IllegalArgumentException("valid observation required");}}
