package com.lumix.withdrawal.request;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

/**
 * P24-T01 的 pure request admission policy。
 *
 * <p>同 user/key 的 payload 不同一律拒絕；amount 上限由外部 asset precision policy 提供，policy 不查詢餘額與不建立 hold。</p>
 */
public final class WithdrawalRequestAdmissionPolicy {

    public WithdrawalRequestAdmissionResult evaluate(
            WithdrawalRequest candidate,
            BigInteger maximumAtomicAmount,
            Map<RequestKey, WithdrawalRequest> existing
    ) {
        candidate = Objects.requireNonNull(candidate, "candidate must not be null");
        maximumAtomicAmount = Objects.requireNonNull(maximumAtomicAmount, "maximumAtomicAmount must not be null");
        existing = Map.copyOf(Objects.requireNonNull(existing, "existing must not be null"));
        if (maximumAtomicAmount.signum() <= 0 || candidate.amount().atoms().compareTo(maximumAtomicAmount) > 0) {
            return new WithdrawalRequestAdmissionResult(WithdrawalRequestAdmissionDecision.AMOUNT_LIMIT_REJECTED, candidate);
        }
        WithdrawalRequest prior = existing.get(new RequestKey(candidate.ownerUserId(), candidate.idempotencyKey()));
        if (prior == null) {
            return new WithdrawalRequestAdmissionResult(WithdrawalRequestAdmissionDecision.ACCEPTED_REQUEST, candidate);
        }
        if (samePayload(prior, candidate)) {
            return new WithdrawalRequestAdmissionResult(WithdrawalRequestAdmissionDecision.DUPLICATE_REPLAY, prior);
        }
        return new WithdrawalRequestAdmissionResult(WithdrawalRequestAdmissionDecision.CONFLICTING_PAYLOAD_REJECTED, prior);
    }

    private static boolean samePayload(WithdrawalRequest left, WithdrawalRequest right) {
        return left.ownerUserId().equals(right.ownerUserId()) && left.asset().equals(right.asset())
                && left.network().equals(right.network()) && left.destination().equals(right.destination())
                && left.amount().equals(right.amount());
    }

    /** idempotency scope 必須包含 owner，避免不同 user 的 client key 相互干擾。 */
    public record RequestKey(com.lumix.account.UserId ownerUserId, WithdrawalRequestIdempotencyKey idempotencyKey) {
        public RequestKey {
            ownerUserId = Objects.requireNonNull(ownerUserId, "ownerUserId must not be null");
            idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        }
    }
}
