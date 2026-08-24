package com.lumix.risk.control;
import java.util.Objects;
/** P26-T05 只檢查 evidence 不可由同一人自核，保留實際 authorization 與 mutation 在後續獨立邊界。 */
public final class RiskPolicyGovernancePolicy { public RiskPolicyGovernanceDecision evaluate(RiskPolicyChangeEvidence evidence) { Objects.requireNonNull(evidence,"evidence"); return evidence.proposer().equals(evidence.reviewer()) ? RiskPolicyGovernanceDecision.INVALID_DUAL_CONTROL_REJECTED : RiskPolicyGovernanceDecision.VALID_DUAL_CONTROL; } }
