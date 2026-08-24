package com.lumix.withdrawal.request.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.lumix.account.*;
import com.lumix.withdrawal.request.*;
import java.math.BigInteger;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class WithdrawalRequestTransitionPolicyTest {
    private final WithdrawalRequestTransitionPolicy policy = new WithdrawalRequestTransitionPolicy();
    @Test void cancelRaceAndDuplicateCancelDoNotOverwriteState() {
        // expected state 是競態防線；舊請求不可覆寫已取消證據，而重送取消必須保持冪等。
        WithdrawalRequestState created = WithdrawalRequestState.created(request());
        WithdrawalRequestTransitionResult cancelled = policy.evaluate(created, WithdrawalRequestLifecycle.REQUESTED, WithdrawalRequestAction.CANCEL, Instant.EPOCH.plusSeconds(1));
        assertEquals(WithdrawalRequestTransitionDecision.TRANSITIONED, cancelled.decision());
        assertEquals(WithdrawalRequestTransitionDecision.STALE_STATE_REJECTED, policy.evaluate(cancelled.effectiveState(), WithdrawalRequestLifecycle.REQUESTED, WithdrawalRequestAction.CANCEL, Instant.EPOCH.plusSeconds(2)).decision());
        assertEquals(WithdrawalRequestTransitionDecision.IDEMPOTENT_NO_CHANGE, policy.evaluate(cancelled.effectiveState(), WithdrawalRequestLifecycle.CANCELLED, WithdrawalRequestAction.CANCEL, Instant.EPOCH.plusSeconds(2)).decision());
    }
    @Test void expireManualReviewAndApprovalHandoffRespectIrreversibleStates() {
        // handoff-ready 後不得以取消回寫審核結論，避免未來 signer 收到可逆且不一致的輸入。
        WithdrawalRequestState state = WithdrawalRequestState.created(request());
        WithdrawalRequestTransitionResult review = policy.evaluate(state, WithdrawalRequestLifecycle.REQUESTED, WithdrawalRequestAction.QUEUE_MANUAL_REVIEW, Instant.EPOCH.plusSeconds(1));
        WithdrawalRequestTransitionResult handoff = policy.evaluate(review.effectiveState(), WithdrawalRequestLifecycle.MANUAL_REVIEW_PENDING, WithdrawalRequestAction.PREPARE_APPROVAL_HANDOFF, Instant.EPOCH.plusSeconds(2));
        assertEquals(WithdrawalRequestLifecycle.APPROVAL_HANDOFF_READY, handoff.effectiveState().lifecycle());
        assertEquals(WithdrawalRequestTransitionDecision.INVALID_TRANSITION_REJECTED, policy.evaluate(handoff.effectiveState(), WithdrawalRequestLifecycle.APPROVAL_HANDOFF_READY, WithdrawalRequestAction.CANCEL, Instant.EPOCH.plusSeconds(3)).decision());
        assertEquals(WithdrawalRequestTransitionDecision.TRANSITIONED, policy.evaluate(state, WithdrawalRequestLifecycle.REQUESTED, WithdrawalRequestAction.EXPIRE, Instant.EPOCH.plusSeconds(1)).decision());
    }
    private static WithdrawalRequest request() { WithdrawalNetwork network = new WithdrawalNetwork("ETH_MAINNET", WithdrawalDestinationFormat.EVM_HEX); WithdrawalRequestId id = new WithdrawalRequestId("req-1"); return new WithdrawalRequest(id, new UserId("user-a"), new AssetSymbol("USDT"), network, WithdrawalDestination.from("0xabcdef0000000000000000000000000000000000", network), new WithdrawalAtomicAmount(BigInteger.ONE), new WithdrawalRequestIdempotencyKey("key-1"), WithdrawalRequestLifecycle.REQUESTED, new WithdrawalAuditEvent(id, WithdrawalAuditEventType.REQUEST_CREATED, Instant.EPOCH)); }
}
