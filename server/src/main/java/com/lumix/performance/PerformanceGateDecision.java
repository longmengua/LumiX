package com.lumix.performance;
/** capacity evidence gate，PASS 不等於 launch 或已執行真實 soak/chaos。 */
public enum PerformanceGateDecision { PASS_EVIDENCE_ONLY, ENVIRONMENT_NOT_ISOLATED_REJECTED, SLO_EXCEEDED_REJECTED, INTEGRITY_UNVERIFIED_REJECTED }
