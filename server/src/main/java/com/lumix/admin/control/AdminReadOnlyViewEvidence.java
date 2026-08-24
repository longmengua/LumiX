package com.lumix.admin.control;
import java.time.Instant; import java.util.Objects;
/** 管理端唯讀畫面使用的 as-of/source-health evidence，避免把 stale 或未知資料呈現成即時真相。 */
public record AdminReadOnlyViewEvidence(String viewReference, String sourceName, boolean sourceHealthy, Instant asOf) { public AdminReadOnlyViewEvidence { viewReference=req(viewReference,"viewReference");sourceName=req(sourceName,"sourceName");asOf=Objects.requireNonNull(asOf,"asOf"); } private static String req(String v,String n){v=Objects.requireNonNull(v,n).trim();if(v.isEmpty())throw new IllegalArgumentException(n+" must not be blank");return v;} }
