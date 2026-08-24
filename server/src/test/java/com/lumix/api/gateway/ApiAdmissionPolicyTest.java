package com.lumix.api.gateway;
import static org.junit.jupiter.api.Assertions.assertEquals; import java.time.Instant; import org.junit.jupiter.api.Test;
class ApiAdmissionPolicyTest { @Test void commandHealthRateAndIdempotencyConflictsFailClosed(){
 // 公開 command 在任何 stale、quota 或 retry payload 分歧下都不可進入 domain。
 ApiAdmissionPolicy policy=new ApiAdmissionPolicy(); assertEquals(ApiAdmissionDecision.HEALTH_NOT_TRUSTED_REJECTED,policy.evaluate(request(ApiHealthState.STALE),1,"","a")); assertEquals(ApiAdmissionDecision.RATE_LIMIT_REJECTED,policy.evaluate(request(ApiHealthState.HEALTHY),0,"","a")); assertEquals(ApiAdmissionDecision.IDEMPOTENCY_CONFLICT_REJECTED,policy.evaluate(request(ApiHealthState.HEALTHY),1,"a","b")); assertEquals(ApiAdmissionDecision.ALLOWED,policy.evaluate(request(ApiHealthState.HEALTHY),1,"a","a")); }
 private static ApiRequestEvidence request(ApiHealthState health){return new ApiRequestEvidence("v1","withdrawal-request","key-1",ApiContractMode.COMMAND,health,Instant.EPOCH);} }
