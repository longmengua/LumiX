package com.lumix.admin.control;
import com.lumix.account.UserId; import java.time.Instant; import java.util.Objects;
/** 稽核匯出請求的最小 immutable evidence；沒有資料內容、檔案輸出或 export access 執行。 */
public record AdminAuditExportEvidence(UserId actor,String reason,String evidenceReference,Instant requestedAt){public AdminAuditExportEvidence{actor=Objects.requireNonNull(actor,"actor");reason=req(reason,"reason");evidenceReference=req(evidenceReference,"evidenceReference");requestedAt=Objects.requireNonNull(requestedAt,"requestedAt");}private static String req(String v,String n){v=Objects.requireNonNull(v,n).trim();if(v.isEmpty())throw new IllegalArgumentException(n+" must not be blank");return v;}}
