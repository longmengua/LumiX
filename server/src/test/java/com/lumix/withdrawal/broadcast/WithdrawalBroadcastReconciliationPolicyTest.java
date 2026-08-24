package com.lumix.withdrawal.broadcast;

import static org.junit.jupiter.api.Assertions.*;

import com.lumix.account.*; import com.lumix.withdrawal.request.*; import com.lumix.withdrawal.request.lifecycle.*; import com.lumix.withdrawal.request.reconciliation.*; import com.lumix.withdrawal.signing.*;
import java.math.BigInteger; import java.time.Instant; import java.util.List;
import org.junit.jupiter.api.Test;

class WithdrawalBroadcastReconciliationPolicyTest {
 private final WithdrawalBroadcastReconciliationPolicy policy = new WithdrawalBroadcastReconciliationPolicy();
 @Test void onlyMatchingSingleAttemptConfirmedEvidenceCanReachFutureHandoff() { // 外部 evidence 必須綁定 intent，且 confirmation 前不會產生完成 handoff。
  WithdrawalSigningIntent intent = intent(); assertTrue(policy.evaluate(intent, List.of(new WithdrawalBroadcastEvidence("attempt-1", intent.intentDigest(), "chain-ref-1", WithdrawalBroadcastStatus.CONFIRMED, Instant.EPOCH))).confirmedForFutureHandoff());
  assertFalse(policy.evaluate(intent, List.of(new WithdrawalBroadcastEvidence("attempt-1", "1".repeat(64), "chain-ref-1", WithdrawalBroadcastStatus.CONFIRMED, Instant.EPOCH))).confirmedForFutureHandoff());
  assertFalse(policy.evaluate(intent, List.of(new WithdrawalBroadcastEvidence("attempt-1", intent.intentDigest(), "chain-ref-1", WithdrawalBroadcastStatus.PENDING_CONFIRMATION, Instant.EPOCH))).confirmedForFutureHandoff());
 }
 private static WithdrawalSigningIntent intent() { WithdrawalRequestId id = new WithdrawalRequestId("request-1"); WithdrawalNetwork n = new WithdrawalNetwork("ETH_MAINNET", WithdrawalDestinationFormat.EVM_HEX); WithdrawalRequest r = new WithdrawalRequest(id,new UserId("owner"),new AssetSymbol("USDT"),n,WithdrawalDestination.from("0xabcdef0000000000000000000000000000000000",n),new WithdrawalAtomicAmount(BigInteger.TEN),new WithdrawalRequestIdempotencyKey("withdraw-1"),WithdrawalRequestLifecycle.REQUESTED,new WithdrawalAuditEvent(id,WithdrawalAuditEventType.REQUEST_CREATED,Instant.EPOCH)); P25WithdrawalSignerInput in = new P25WithdrawalSignerInput(new WithdrawalRequestState(r,WithdrawalRequestLifecycle.APPROVAL_HANDOFF_READY,List.of(r.createdAuditEvent())),new WithdrawalHoldEvidence(id,"hold-1"),"0".repeat(64)); return new WithdrawalSigningIntent(in,new WithdrawalSigningIntentIdempotencyKey("sign-1"),"0".repeat(64)); }
}
