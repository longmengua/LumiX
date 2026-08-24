package com.lumix.audit.evidence;
import static org.junit.jupiter.api.Assertions.assertEquals; import java.time.Instant; import java.util.List; import org.junit.jupiter.api.Test;
class ReadOnlyAuditProjectionPolicyTest { @Test void gapOrUnknownEvidenceFailsClosed(){
 // audit projection 不能把缺口當作空集合，否則後續 reconciliation/export 會產生錯誤完整性宣稱。
 ReadOnlyAuditProjectionPolicy policy=new ReadOnlyAuditProjectionPolicy(); assertEquals(AuditProjectionDecision.EVIDENCE_GAP_REJECTED,policy.evaluate(List.of(evidence(AuditEvidenceCompleteness.GAP)))); assertEquals(AuditProjectionDecision.EVIDENCE_GAP_REJECTED,policy.evaluate(List.of(evidence(AuditEvidenceCompleteness.UNKNOWN)))); assertEquals(AuditProjectionDecision.AVAILABLE,policy.evaluate(List.of(evidence(AuditEvidenceCompleteness.COMPLETE)))); }
 private static AuditEvidence evidence(AuditEvidenceCompleteness c){return new AuditEvidence("corr-1","risk","v1","0".repeat(64),c,Instant.EPOCH);} }
