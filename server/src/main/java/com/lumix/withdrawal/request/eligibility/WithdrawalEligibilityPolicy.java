package com.lumix.withdrawal.request.eligibility;

import com.lumix.withdrawal.request.WithdrawalRequest;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * P24-T02 的 pure eligibility policy；併發中的既有 future hold 必須顯式列入可用額度計算。
 *
 * <p>本 policy 不讀取 balance、不操作 reservation，僅將不足額、過期 quote 與風控不確定轉為 fail-closed reason。</p>
 */
public final class WithdrawalEligibilityPolicy {
    public WithdrawalEligibilityResult evaluate(
            WithdrawalRequest request,
            WithdrawalAvailableBalanceEvidence availableBalance,
            WithdrawalFeeQuote quote,
            WithdrawalRiskDecision risk,
            Instant evaluatedAt,
            Collection<WithdrawalHoldHandoff> pendingHandoffs
    ) {
        WithdrawalRequest withdrawalRequest = Objects.requireNonNull(request, "request must not be null");
        WithdrawalAvailableBalanceEvidence balance = Objects.requireNonNull(availableBalance, "availableBalance must not be null");
        WithdrawalFeeQuote feeQuote = Objects.requireNonNull(quote, "quote must not be null");
        WithdrawalRiskDecision riskDecision = Objects.requireNonNull(risk, "risk must not be null");
        Instant evaluationTime = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        List<WithdrawalHoldHandoff> currentPendingHandoffs = List.copyOf(
                Objects.requireNonNull(pendingHandoffs, "pendingHandoffs must not be null"));
        if (!withdrawalRequest.asset().equals(feeQuote.asset()) || !withdrawalRequest.network().equals(feeQuote.network())
                || !withdrawalRequest.ownerUserId().equals(balance.ownerUserId()) || !withdrawalRequest.asset().equals(balance.asset())) {
            return denied(WithdrawalEligibilityReason.FEE_QUOTE_MISMATCH);
        }
        if (!evaluationTime.isBefore(feeQuote.expiresAt())) return denied(WithdrawalEligibilityReason.FEE_QUOTE_EXPIRED);
        if (riskDecision == WithdrawalRiskDecision.REJECTED) return denied(WithdrawalEligibilityReason.RISK_REJECTED);
        if (riskDecision == WithdrawalRiskDecision.UNKNOWN) return denied(WithdrawalEligibilityReason.RISK_UNKNOWN);
        BigInteger required = withdrawalRequest.amount().atoms().add(feeQuote.fee().atoms());
        BigInteger pending = currentPendingHandoffs.stream()
                .filter(handoff -> handoff.request().ownerUserId().equals(withdrawalRequest.ownerUserId())
                        && handoff.request().asset().equals(withdrawalRequest.asset()))
                .map(WithdrawalHoldHandoff::requiredAtomicAmount).reduce(BigInteger.ZERO, BigInteger::add);
        if (required.add(pending).compareTo(balance.availableAtomicAmount()) > 0) {
            return denied(WithdrawalEligibilityReason.INSUFFICIENT_AVAILABLE_BALANCE);
        }
        return new WithdrawalEligibilityResult(WithdrawalEligibilityReason.ELIGIBLE_FOR_FUTURE_HOLD,
                Optional.of(new WithdrawalHoldHandoff(withdrawalRequest, feeQuote, required)));
    }
    private static WithdrawalEligibilityResult denied(WithdrawalEligibilityReason reason) { return new WithdrawalEligibilityResult(reason, Optional.empty()); }
}
