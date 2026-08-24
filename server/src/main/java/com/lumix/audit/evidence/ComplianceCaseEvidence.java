package com.lumix.audit.evidence;
import java.time.Instant; import java.util.Objects;
/** case/review/escalation 的不可變 reference；不含 KYC/AML bypass、自動處分或資產處置。 */
public record ComplianceCaseEvidence(String caseReference,String evidenceCorrelationId,boolean escalationRequired,Instant recordedAt){public ComplianceCaseEvidence{caseReference=req(caseReference,"caseReference");evidenceCorrelationId=req(evidenceCorrelationId,"evidenceCorrelationId");recordedAt=Objects.requireNonNull(recordedAt,"recordedAt");}private static String req(String v,String n){v=Objects.requireNonNull(v,n).trim();if(v.isEmpty())throw new IllegalArgumentException(n+" must not be blank");return v;}}
