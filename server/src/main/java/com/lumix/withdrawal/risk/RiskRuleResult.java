package com.lumix.withdrawal.risk;

import java.util.Map;

/** 單一規則結果；evidence 僅允許去敏後的操作性資料。 */
public record RiskRuleResult(String ruleCode, int scoreDelta, RiskDecision decision, Map<String, Object> evidence) { }
