package com.lumix.withdrawal.signing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.withdrawal.request.*;
import com.lumix.withdrawal.request.lifecycle.WithdrawalRequestState;
import com.lumix.withdrawal.request.reconciliation.*;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WithdrawalSignerDispatchPolicyTest {
    private final WithdrawalSignerDispatchPolicy policy = new WithdrawalSignerDispatchPolicy();
    @Test void unavailableOrWrongNetworkCapabilityNeverCreatesAdapterEnvelope() {
        // capability 檢查在 adapter 前完成，避免不相容或下線 signer 收到任何可處理的 intent。
        WithdrawalSigningIntent intent = intent();
        assertEquals(WithdrawalSignerDispatchDecision.CAPABILITY_UNAVAILABLE_REJECTED, policy.evaluate(intent, new WithdrawalSignerCapability("hsm-a", WithdrawalSignerProviderKind.HSM, Set.of("ETH_MAINNET"), false), Duration.ofSeconds(5), 1).decision());
        assertEquals(WithdrawalSignerDispatchDecision.NETWORK_UNSUPPORTED_REJECTED, policy.evaluate(intent, new WithdrawalSignerCapability("hsm-a", WithdrawalSignerProviderKind.HSM, Set.of("BTC_MAINNET"), true), Duration.ofSeconds(5), 1).decision());
        assertEquals(WithdrawalSignerDispatchDecision.READY_FOR_ISOLATED_ADAPTER, policy.evaluate(intent, new WithdrawalSignerCapability("hsm-a", WithdrawalSignerProviderKind.HSM, Set.of("ETH_MAINNET"), true), Duration.ofSeconds(5), 1).decision());
    }
    private static WithdrawalSigningIntent intent() {
        WithdrawalRequestId id = new WithdrawalRequestId("request-1"); WithdrawalNetwork network = new WithdrawalNetwork("ETH_MAINNET", WithdrawalDestinationFormat.EVM_HEX);
        WithdrawalRequest request = new WithdrawalRequest(id, new UserId("owner"), new AssetSymbol("USDT"), network, WithdrawalDestination.from("0xabcdef0000000000000000000000000000000000", network), new WithdrawalAtomicAmount(BigInteger.TEN), new WithdrawalRequestIdempotencyKey("withdraw-1"), WithdrawalRequestLifecycle.REQUESTED, new WithdrawalAuditEvent(id, WithdrawalAuditEventType.REQUEST_CREATED, Instant.EPOCH));
        P25WithdrawalSignerInput input = new P25WithdrawalSignerInput(new WithdrawalRequestState(request, WithdrawalRequestLifecycle.APPROVAL_HANDOFF_READY, List.of(request.createdAuditEvent())), new WithdrawalHoldEvidence(id, "hold-1"), "0".repeat(64));
        return new WithdrawalSigningIntent(input, new WithdrawalSigningIntentIdempotencyKey("sign-1"), "1".repeat(64));
    }
}
