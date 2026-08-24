package com.lumix.security.evidence;
/** security launch gate outcome；PASS 只是 evidence completeness，不能單獨構成 production sign-off。 */
public enum SecurityLaunchGateDecision { PASS_EVIDENCE_ONLY, CRITICAL_FINDING_REJECTED, EXCEPTION_EXPIRED_REJECTED }
