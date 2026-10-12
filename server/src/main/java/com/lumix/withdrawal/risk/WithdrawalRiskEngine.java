package com.lumix.withdrawal.risk;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** 聚合獨立規則；DENY/HOLD 永遠優先於較低風險的 allow 結果。 */
@Service
public class WithdrawalRiskEngine {
    private final List<WithdrawalRiskRule> rules;
    public WithdrawalRiskEngine(List<WithdrawalRiskRule> rules) { this.rules=List.copyOf(Objects.requireNonNull(rules)); }
    public Assessment evaluate(WithdrawalRiskContext context) {
        List<RiskRuleResult> results=rules.stream().map(rule->rule.evaluate(context)).toList();
        RiskDecision decision=results.stream().map(RiskRuleResult::decision).max(Comparator.comparingInt(this::severity)).orElse(RiskDecision.HOLD);
        return new Assessment(decision,results.stream().mapToInt(RiskRuleResult::scoreDelta).sum(),results);
    }
    private int severity(RiskDecision decision) { return switch(decision) { case ALLOW -> 0; case MANUAL_REVIEW -> 1; case HOLD -> 2; case DENY -> 3; }; }
    public record Assessment(RiskDecision decision,int score,List<RiskRuleResult> triggers) { }
}
