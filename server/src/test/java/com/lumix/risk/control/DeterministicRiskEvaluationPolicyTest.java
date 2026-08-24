package com.lumix.risk.control;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DeterministicRiskEvaluationPolicyTest {
    private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");
    private final DeterministicRiskEvaluationPolicy policy = new DeterministicRiskEvaluationPolicy();

    @Test
    void staleHaltedAndOverLimitInputsFailClosed() {
        // 不可信或保護中資料絕不可因額度尚足而被誤判允許。
        RiskPolicy riskPolicy = new RiskPolicy("risk-v1", BigInteger.TEN, NOW.minusSeconds(1));
        assertEquals(RiskDecision.INPUT_NOT_TRUSTED_REJECTED, policy.evaluate(riskPolicy, input(BigInteger.ONE, RiskInputFreshness.STALE, RiskMarketState.NORMAL)).decision());
        assertEquals(RiskDecision.MARKET_PROTECTION_REJECTED, policy.evaluate(riskPolicy, input(BigInteger.ONE, RiskInputFreshness.FRESH, RiskMarketState.HALTED)).decision());
        assertEquals(RiskDecision.LIMIT_EXCEEDED_REJECTED, policy.evaluate(riskPolicy, input(BigInteger.valueOf(11), RiskInputFreshness.FRESH, RiskMarketState.NORMAL)).decision());
        assertEquals(RiskDecision.ALLOWED, policy.evaluate(riskPolicy, input(BigInteger.TEN, RiskInputFreshness.FRESH, RiskMarketState.NORMAL)).decision());
    }

    private static RiskEvaluationInput input(BigInteger amount, RiskInputFreshness freshness, RiskMarketState market) {
        return new RiskEvaluationInput(RiskAction.WITHDRAWAL_REQUEST, amount, freshness, market, NOW);
    }
}
