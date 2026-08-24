package com.lumix.withdrawal.signing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.withdrawal.approval.WithdrawalApprovalDecision;
import com.lumix.withdrawal.approval.WithdrawalApprovalResult;
import com.lumix.withdrawal.request.WithdrawalAtomicAmount;
import com.lumix.withdrawal.request.WithdrawalAuditEvent;
import com.lumix.withdrawal.request.WithdrawalAuditEventType;
import com.lumix.withdrawal.request.WithdrawalDestination;
import com.lumix.withdrawal.request.WithdrawalDestinationFormat;
import com.lumix.withdrawal.request.WithdrawalNetwork;
import com.lumix.withdrawal.request.WithdrawalRequest;
import com.lumix.withdrawal.request.WithdrawalRequestId;
import com.lumix.withdrawal.request.WithdrawalRequestIdempotencyKey;
import com.lumix.withdrawal.request.WithdrawalRequestLifecycle;
import com.lumix.withdrawal.request.lifecycle.WithdrawalRequestState;
import com.lumix.withdrawal.request.reconciliation.P25WithdrawalSignerInput;
import com.lumix.withdrawal.request.reconciliation.WithdrawalHoldEvidence;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WithdrawalSigningIntentPolicyTest {

    private final WithdrawalSigningIntentPolicy policy = new WithdrawalSigningIntentPolicy();
    private final WithdrawalSigningIntentIdempotencyKey key = new WithdrawalSigningIntentIdempotencyKey("sign-1");

    @Test
    void approvedInputCreatesReplayableIntentButConflictingPayloadFailsClosed() {
        // digest 綁定 audit evidence；同一 request/key 的不同 evidence 不可因 retry 被靜默覆寫。
        WithdrawalSigningIntentResult created = policy.evaluate(input("0"), approved(), key, Map.of());
        WithdrawalSigningIntent existing = created.signingIntent().orElseThrow();
        Map<WithdrawalSigningIntentPolicy.SigningIntentKey, WithdrawalSigningIntent> persisted = Map.of(
                new WithdrawalSigningIntentPolicy.SigningIntentKey(existing.signerInput().requestState().request().requestId(), key), existing);

        assertEquals(WithdrawalSigningIntentDecision.INTENT_CREATED, created.decision());
        assertEquals(WithdrawalSigningIntentDecision.DUPLICATE_REPLAY, policy.evaluate(input("0"), approved(), key, persisted).decision());
        assertEquals(WithdrawalSigningIntentDecision.CONFLICTING_IDEMPOTENCY_PAYLOAD_REJECTED,
                policy.evaluate(input("1"), approved(), key, persisted).decision());
    }

    @Test
    void unapprovedResultNeverProducesSigningIntent() {
        // signing intent 一律以 approval outcome 為前置，不能由 caller 直接跳過審核。
        WithdrawalSigningIntentResult result = policy.evaluate(input("0"),
                new WithdrawalApprovalResult(WithdrawalApprovalDecision.MISSING_REQUIRED_ROLE_REJECTED, List.of()), key, Map.of());

        assertEquals(WithdrawalSigningIntentDecision.APPROVAL_NOT_GRANTED_REJECTED, result.decision());
        assertFalse(result.signingIntent().isPresent());
    }

    private static WithdrawalApprovalResult approved() {
        return new WithdrawalApprovalResult(WithdrawalApprovalDecision.APPROVED, List.of());
    }

    private static P25WithdrawalSignerInput input(String auditLastHex) {
        WithdrawalRequestId id = new WithdrawalRequestId("request-1");
        WithdrawalNetwork network = new WithdrawalNetwork("ETH_MAINNET", WithdrawalDestinationFormat.EVM_HEX);
        WithdrawalRequest request = new WithdrawalRequest(id, new UserId("owner"), new AssetSymbol("USDT"), network,
                WithdrawalDestination.from("0xabcdef0000000000000000000000000000000000", network), new WithdrawalAtomicAmount(BigInteger.TEN),
                new WithdrawalRequestIdempotencyKey("withdraw-1"), WithdrawalRequestLifecycle.REQUESTED,
                new WithdrawalAuditEvent(id, WithdrawalAuditEventType.REQUEST_CREATED, Instant.EPOCH));
        WithdrawalRequestState state = new WithdrawalRequestState(request, WithdrawalRequestLifecycle.APPROVAL_HANDOFF_READY, List.of(request.createdAuditEvent()));
        return new P25WithdrawalSignerInput(state, new WithdrawalHoldEvidence(id, "hold-1"), "0".repeat(63) + auditLastHex);
    }
}
