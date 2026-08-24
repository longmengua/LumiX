package com.lumix.security.evidence;
import static org.junit.jupiter.api.Assertions.assertEquals; import com.lumix.account.UserId; import java.time.Instant; import org.junit.jupiter.api.Test;
class SecurityLaunchGatePolicyTest { @Test void criticalOrExpiredExceptionFailsClosed(){
 // 重大 finding 與過期例外不可被自動接受，需保持在安全審核與修復流程中。
 SecurityLaunchGatePolicy policy=new SecurityLaunchGatePolicy(); Instant now=Instant.EPOCH; SecurityRemediationEvidence remediation=new SecurityRemediationEvidence("f-1",new UserId("a"),new UserId("b"),now.plusSeconds(1)); assertEquals(SecurityLaunchGateDecision.CRITICAL_FINDING_REJECTED,policy.evaluate(new SecurityFindingEvidence("f-1","signer",SecurityFindingSeverity.CRITICAL,now),remediation,now)); assertEquals(SecurityLaunchGateDecision.EXCEPTION_EXPIRED_REJECTED,policy.evaluate(new SecurityFindingEvidence("f-1","api",SecurityFindingSeverity.HIGH,now),remediation,now.plusSeconds(1))); }
}
