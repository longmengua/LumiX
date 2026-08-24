package com.lumix.security.evidence;
import com.lumix.account.UserId; import java.time.Instant; import java.util.Objects;
/** remediation/exception 的雙人 evidence；exception 必須有期限，且不會直接改變 security control。 */
public record SecurityRemediationEvidence(String findingReference,UserId proposer,UserId reviewer,Instant expiresAt){public SecurityRemediationEvidence{findingReference=Objects.requireNonNull(findingReference,"findingReference").trim();proposer=Objects.requireNonNull(proposer,"proposer");reviewer=Objects.requireNonNull(reviewer,"reviewer");expiresAt=Objects.requireNonNull(expiresAt,"expiresAt");if(findingReference.isEmpty()||proposer.equals(reviewer))throw new IllegalArgumentException("remediation requires reference and separated review");}}
