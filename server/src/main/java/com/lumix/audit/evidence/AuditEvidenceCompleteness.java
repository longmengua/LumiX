package com.lumix.audit.evidence;
/** 證據完整度；GAP/UNKNOWN 只能升級調查，絕不可被 projection 或 export 靜默忽略。 */
public enum AuditEvidenceCompleteness { COMPLETE, GAP, UNKNOWN }
