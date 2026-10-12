package com.lumix.withdrawal.risk;

/** 每條提款風控規則獨立，避免單一巨型 risk service 無法稽核或安全演進。 */
public interface WithdrawalRiskRule { RiskRuleResult evaluate(WithdrawalRiskContext context); }
