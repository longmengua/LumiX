package com.lumix.risk.control;
import com.lumix.account.UserId; import java.time.Instant; import java.util.Objects;
/** policy change 的 immutable dual-control evidence；不實作角色授與、覆寫或 policy persistence。 */
public record RiskPolicyChangeEvidence(String policyVersion, UserId proposer, UserId reviewer, Instant reviewedAt) { public RiskPolicyChangeEvidence { policyVersion=Objects.requireNonNull(policyVersion,"policyVersion").trim(); proposer=Objects.requireNonNull(proposer,"proposer"); reviewer=Objects.requireNonNull(reviewer,"reviewer"); reviewedAt=Objects.requireNonNull(reviewedAt,"reviewedAt"); if(policyVersion.isEmpty()||proposer.equals(reviewer)) throw new IllegalArgumentException("policy change requires version and distinct dual control"); } }
