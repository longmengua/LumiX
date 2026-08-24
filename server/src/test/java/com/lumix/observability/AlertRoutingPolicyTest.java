package com.lumix.observability;
import static org.junit.jupiter.api.Assertions.assertEquals; import java.time.Instant; import org.junit.jupiter.api.Test;
class AlertRoutingPolicyTest { @Test void criticalSignalWithoutRunbookFailsClosed(){
 // 重大資金或安全 signal 沒有可執行 runbook 時，不可當作已告警處理。
 AlertRoutingPolicy policy=new AlertRoutingPolicy(); OperationalSignal critical=new OperationalSignal("corr-1","withdrawal","signer-latency",OperationalSignalSeverity.CRITICAL,Instant.EPOCH); assertEquals(AlertRoutingDecision.MISSING_TELEMETRY_REJECTED,policy.evaluate(critical,"")); assertEquals(AlertRoutingDecision.ESCALATE_WITH_RUNBOOK,policy.evaluate(critical,"runbook-withdrawal")); }
}
