package com.lumix.launch;
import java.time.Instant; import java.util.Objects;
/** 單一 gate 的 evidence reference；passed=false 明確保留未完成狀態，不能被文件或測試取代。 */
public record LaunchGateEvidence(LaunchReadinessGate gate,String evidenceReference,boolean passed,Instant assessedAt){public LaunchGateEvidence{gate=Objects.requireNonNull(gate,"gate");evidenceReference=Objects.requireNonNull(evidenceReference,"evidenceReference").trim();assessedAt=Objects.requireNonNull(assessedAt,"assessedAt");if(evidenceReference.isEmpty())throw new IllegalArgumentException("evidence reference must not be blank");}}
