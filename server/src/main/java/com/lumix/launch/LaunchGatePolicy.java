package com.lumix.launch;
import java.util.List; import java.util.Objects;
/** P36 只彙整權威門檻 evidence；它不能產生 sign-off、kill switch、launch、rollback 或 production-ready claim。 */
public final class LaunchGatePolicy { public LaunchGateDecision evaluate(List<LaunchGateEvidence> evidence,boolean humanSignedOff){List<LaunchGateEvidence> immutableEvidence=List.copyOf(Objects.requireNonNull(evidence,"evidence"));for(LaunchReadinessGate gate:LaunchReadinessGate.values()){boolean passed=false;for(LaunchGateEvidence item:immutableEvidence){if(item.gate()==gate&&item.passed()){passed=true;break;}}if(!passed)return LaunchGateDecision.NOT_READY_EVIDENCE_MISSING;}return humanSignedOff?LaunchGateDecision.READY_FOR_HUMAN_SIGN_OFF_ONLY:LaunchGateDecision.NOT_READY_HUMAN_SIGN_OFF_MISSING;} }
