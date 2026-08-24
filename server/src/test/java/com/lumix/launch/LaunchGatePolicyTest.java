package com.lumix.launch;
import static org.junit.jupiter.api.Assertions.assertEquals; import java.time.Instant; import java.util.List; import org.junit.jupiter.api.Test;
class LaunchGatePolicyTest { @Test void allEvidenceStillRequiresActualHumanSignOff(){
 // foundation/test evidence 與任何 agent 決策都不可取代獨立人類簽核。
 LaunchGatePolicy policy=new LaunchGatePolicy(); List<LaunchGateEvidence> all=java.util.Arrays.stream(LaunchReadinessGate.values()).map(gate->new LaunchGateEvidence(gate,"e-"+gate,true,Instant.EPOCH)).toList(); assertEquals(LaunchGateDecision.NOT_READY_HUMAN_SIGN_OFF_MISSING,policy.evaluate(all,false)); assertEquals(LaunchGateDecision.NOT_READY_EVIDENCE_MISSING,policy.evaluate(all.subList(0,all.size()-1),true)); }
}
