package com.lumix.operations;
import static org.junit.jupiter.api.Assertions.assertEquals; import java.time.Instant; import java.util.List; import org.junit.jupiter.api.Test;
class OperationalReadinessPolicyTest { @Test void anyUnresolvedOperationalGapBlocksHumanGoNoGo(){
 // 組織/合規/支援缺口不能由工程 agent 或單一技術指標宣稱已關閉。
 OperationalReadinessPolicy policy=new OperationalReadinessPolicy(); OperationalOwnershipEvidence evidence=new OperationalOwnershipEvidence("exchange","on-call","runbook",Instant.EPOCH); assertEquals(OperationalReadinessDecision.OPEN_GAP_REJECTED,policy.evaluate(evidence,List.of(OperationalGap.COMPLIANCE_REVIEW_MISSING))); assertEquals(OperationalReadinessDecision.READY_FOR_HUMAN_GO_NO_GO,policy.evaluate(evidence,List.of(OperationalGap.NONE))); }
}
