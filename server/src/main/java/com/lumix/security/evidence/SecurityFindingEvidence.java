package com.lumix.security.evidence;
import java.time.Instant; import java.util.Objects;
/** secret-free threat/finding evidence，僅保留受控 reference；不得記錄 credential、key 或 exploit payload。 */
public record SecurityFindingEvidence(String findingReference,String threatBoundary,SecurityFindingSeverity severity,Instant discoveredAt){public SecurityFindingEvidence{findingReference=req(findingReference,"findingReference");threatBoundary=req(threatBoundary,"threatBoundary");severity=Objects.requireNonNull(severity,"severity");discoveredAt=Objects.requireNonNull(discoveredAt,"discoveredAt");}private static String req(String v,String n){v=Objects.requireNonNull(v,n).trim();if(v.isEmpty())throw new IllegalArgumentException(n+" must not be blank");return v;}}
