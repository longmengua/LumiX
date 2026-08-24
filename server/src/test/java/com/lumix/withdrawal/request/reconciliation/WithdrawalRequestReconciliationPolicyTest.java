package com.lumix.withdrawal.request.reconciliation;

import static org.junit.jupiter.api.Assertions.*;
import com.lumix.account.*; import com.lumix.withdrawal.request.*; import com.lumix.withdrawal.request.lifecycle.*;
import java.math.BigInteger; import java.time.Instant; import java.util.Optional; import org.junit.jupiter.api.Test;
class WithdrawalRequestReconciliationPolicyTest {
 private final WithdrawalRequestReconciliationPolicy policy = new WithdrawalRequestReconciliationPolicy();
 @Test void replayableApprovalStateWithHoldProducesKeylessP25Input() { // 相同 immutable evidence 重放時 digest 必須固定，P25 才能可靠稽核而非重新解釋 request。
  WithdrawalRequestState ready = ready(); WithdrawalRequestReconciliationReport one = policy.evaluate(ready, Optional.of(new WithdrawalHoldEvidence(ready.request().requestId(), "hold-1"))); WithdrawalRequestReconciliationReport two = policy.evaluate(ready, Optional.of(new WithdrawalHoldEvidence(ready.request().requestId(), "hold-1"))); assertTrue(one.signerInput().isPresent()); assertEquals(one.signerInput().orElseThrow().auditDigest(), two.signerInput().orElseThrow().auditDigest()); }
 @Test void missingHoldOrUnreadyStateFailsClosed() { // hold 或 approval handoff 缺失時，絕不可產生可交給 signer 的資料。
  WithdrawalRequestState ready = ready(); assertFalse(policy.evaluate(ready, Optional.empty()).signerInput().isPresent()); WithdrawalRequestState created = WithdrawalRequestState.created(request()); assertTrue(policy.evaluate(created, Optional.of(new WithdrawalHoldEvidence(created.request().requestId(), "hold-1"))).exceptions().contains(WithdrawalReconciliationException.NOT_READY_FOR_SIGNER)); }
 private WithdrawalRequestState ready() { WithdrawalRequestTransitionPolicy transitions = new WithdrawalRequestTransitionPolicy(); WithdrawalRequestState created = WithdrawalRequestState.created(request()); WithdrawalRequestState review = transitions.evaluate(created, WithdrawalRequestLifecycle.REQUESTED, WithdrawalRequestAction.QUEUE_MANUAL_REVIEW, Instant.EPOCH.plusSeconds(1)).effectiveState(); return transitions.evaluate(review, WithdrawalRequestLifecycle.MANUAL_REVIEW_PENDING, WithdrawalRequestAction.PREPARE_APPROVAL_HANDOFF, Instant.EPOCH.plusSeconds(2)).effectiveState(); }
 private static WithdrawalRequest request() { WithdrawalNetwork n = new WithdrawalNetwork("ETH_MAINNET", WithdrawalDestinationFormat.EVM_HEX); WithdrawalRequestId id = new WithdrawalRequestId("req-1"); return new WithdrawalRequest(id,new UserId("user-a"),new AssetSymbol("USDT"),n,WithdrawalDestination.from("0xabcdef0000000000000000000000000000000000",n),new WithdrawalAtomicAmount(BigInteger.ONE),new WithdrawalRequestIdempotencyKey("key-1"),WithdrawalRequestLifecycle.REQUESTED,new WithdrawalAuditEvent(id,WithdrawalAuditEventType.REQUEST_CREATED,Instant.EPOCH)); }
}
