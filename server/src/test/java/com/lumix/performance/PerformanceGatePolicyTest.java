package com.lumix.performance;
import static org.junit.jupiter.api.Assertions.assertEquals; import java.time.Instant; import org.junit.jupiter.api.Test;
class PerformanceGatePolicyTest { @Test void isolationSloAndIntegrityAreMandatory(){
 // 單次低延遲 benchmark 不足以替代隔離與資料完整性驗證。
 PerformanceGatePolicy policy=new PerformanceGatePolicy(); PerformanceObservation observation=new PerformanceObservation("api-soak",10,0,true,Instant.EPOCH); assertEquals(PerformanceGateDecision.ENVIRONMENT_NOT_ISOLATED_REJECTED,policy.evaluate(new WorkloadProfile("p",1,false),observation,20)); assertEquals(PerformanceGateDecision.SLO_EXCEEDED_REJECTED,policy.evaluate(new WorkloadProfile("p",1,true),new PerformanceObservation("api",21,0,true,Instant.EPOCH),20)); assertEquals(PerformanceGateDecision.INTEGRITY_UNVERIFIED_REJECTED,policy.evaluate(new WorkloadProfile("p",1,true),new PerformanceObservation("api",10,0,false,Instant.EPOCH),20)); }
}
