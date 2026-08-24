package com.lumix.audit.evidence;
import java.util.List; import java.util.Objects;
/** P28 projection gate，只檢查完整度，不查詢或匯出原始資料。 */
public final class ReadOnlyAuditProjectionPolicy { public AuditProjectionDecision evaluate(List<AuditEvidence> evidence){evidence=List.copyOf(Objects.requireNonNull(evidence,"evidence"));return evidence.stream().allMatch(item->item.completeness()==AuditEvidenceCompleteness.COMPLETE)?AuditProjectionDecision.AVAILABLE:AuditProjectionDecision.EVIDENCE_GAP_REJECTED;} }
