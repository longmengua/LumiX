package com.lumix.operations;
import java.time.Instant; import java.util.Objects;
/** on-call/support/incident ownership reference；不取代真人責任、contact directory 或排班系統。 */
public record OperationalOwnershipEvidence(String serviceReference,String ownerReference,String runbookReference,Instant verifiedAt){public OperationalOwnershipEvidence{serviceReference=req(serviceReference,"serviceReference");ownerReference=req(ownerReference,"ownerReference");runbookReference=req(runbookReference,"runbookReference");verifiedAt=Objects.requireNonNull(verifiedAt,"verifiedAt");}private static String req(String v,String n){v=Objects.requireNonNull(v,n).trim();if(v.isEmpty())throw new IllegalArgumentException(n+" must not be blank");return v;}}
