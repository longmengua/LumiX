package com.lumix.observability;
import java.time.Instant; import java.util.Objects;
/** 結構化 domain signal，只保留 correlation/reference；不得放入 secret、token 或未遮罩個資。 */
public record OperationalSignal(String correlationId,String domain,String metricReference,OperationalSignalSeverity severity,Instant observedAt){public OperationalSignal{correlationId=req(correlationId,"correlationId");domain=req(domain,"domain");metricReference=req(metricReference,"metricReference");severity=Objects.requireNonNull(severity,"severity");observedAt=Objects.requireNonNull(observedAt,"observedAt");}private static String req(String v,String n){v=Objects.requireNonNull(v,n).trim();if(v.isEmpty())throw new IllegalArgumentException(n+" must not be blank");return v;}}
