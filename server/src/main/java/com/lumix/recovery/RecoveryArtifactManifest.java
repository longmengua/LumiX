package com.lumix.recovery;
import java.time.Instant; import java.util.Objects;
/** backup/replay artifact 的 immutable manifest；只驗證 reference/integrity，不讀寫備份或金鑰。 */
public record RecoveryArtifactManifest(String artifactReference,String integrityDigest,Instant capturedAt){public RecoveryArtifactManifest{artifactReference=req(artifactReference,"artifactReference");integrityDigest=req(integrityDigest,"integrityDigest");capturedAt=Objects.requireNonNull(capturedAt,"capturedAt");if(!integrityDigest.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("integrity digest must be SHA-256 hex");}private static String req(String v,String n){v=Objects.requireNonNull(v,n).trim();if(v.isEmpty())throw new IllegalArgumentException(n+" must not be blank");return v;}}
