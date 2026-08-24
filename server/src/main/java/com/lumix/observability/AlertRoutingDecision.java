package com.lumix.observability;
/** alert routing outcome；ESCALATE 是 runbook evidence，不會呼叫 provider 或通知人員。 */
public enum AlertRoutingDecision { RECORD_ONLY, ESCALATE_WITH_RUNBOOK, MISSING_TELEMETRY_REJECTED }
