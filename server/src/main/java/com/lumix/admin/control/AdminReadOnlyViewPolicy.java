package com.lumix.admin.control;
import java.util.Objects;
/** 只依 source health 決定是否可展示 evidence，不查詢資料或輸出 endpoint。 */
public final class AdminReadOnlyViewPolicy { public AdminReadOnlyViewDecision evaluate(AdminReadOnlyViewEvidence evidence){return Objects.requireNonNull(evidence,"evidence").sourceHealthy()?AdminReadOnlyViewDecision.VIEW_AVAILABLE:AdminReadOnlyViewDecision.SOURCE_UNHEALTHY_REJECTED;} }
