package com.lumix.withdrawal.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WithdrawalRequestAdmissionPolicyTest {

    private final WithdrawalRequestAdmissionPolicy policy = new WithdrawalRequestAdmissionPolicy();
    private final WithdrawalNetwork ethereum = new WithdrawalNetwork("ETH_MAINNET", WithdrawalDestinationFormat.EVM_HEX);

    @Test
    void retryIsIdempotentButSameKeyDifferentPayloadFailsClosed() {
        // 同一 owner/key 的安全重試只能重放完整相同 payload，避免 client key 被竄改後覆蓋原始請求。
        WithdrawalRequest request = request(ethereum, "10", "0xabcdef0000000000000000000000000000000000");
        Map<WithdrawalRequestAdmissionPolicy.RequestKey, WithdrawalRequest> existing = Map.of(
                new WithdrawalRequestAdmissionPolicy.RequestKey(request.ownerUserId(), request.idempotencyKey()), request);

        assertEquals(WithdrawalRequestAdmissionDecision.DUPLICATE_REPLAY,
                policy.evaluate(request, BigInteger.TEN, existing).decision());
        assertEquals(WithdrawalRequestAdmissionDecision.CONFLICTING_PAYLOAD_REJECTED,
                policy.evaluate(request(ethereum, "9", "0xabcdef0000000000000000000000000000000000"), BigInteger.TEN, existing).decision());
    }

    @Test
    void wrongNetworkAddressFormatAndAmountLimitFailClosed() {
        // destination 格式或上游額度不可信時，admission 必須在尚未建立任何資金副作用前拒絕。
        WithdrawalNetwork bitcoin = new WithdrawalNetwork("BTC_MAINNET", WithdrawalDestinationFormat.BECH32);
        assertThrows(IllegalArgumentException.class, () -> new WithdrawalRequest(
                new WithdrawalRequestId("request-wrong-network"), new UserId("user-a"), new AssetSymbol("USDT"), ethereum,
                WithdrawalDestination.from("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kygt080", bitcoin), new WithdrawalAtomicAmount(BigInteger.ONE),
                new WithdrawalRequestIdempotencyKey("withdraw-a"), WithdrawalRequestLifecycle.REQUESTED,
                new WithdrawalAuditEvent(new WithdrawalRequestId("request-wrong-network"), WithdrawalAuditEventType.REQUEST_CREATED, Instant.EPOCH)));
        assertEquals(WithdrawalRequestAdmissionDecision.AMOUNT_LIMIT_REJECTED,
                policy.evaluate(request(ethereum, "11", "0xabcdef0000000000000000000000000000000000"), BigInteger.TEN, Map.of()).decision());
    }

    private static WithdrawalRequest request(WithdrawalNetwork network, String amount, String destination) {
        WithdrawalRequestId id = new WithdrawalRequestId("request-1");
        return new WithdrawalRequest(id, new UserId("user-a"), new AssetSymbol("USDT"), network,
                WithdrawalDestination.from(destination, network), new WithdrawalAtomicAmount(new BigInteger(amount)),
                new WithdrawalRequestIdempotencyKey("withdraw-a"), WithdrawalRequestLifecycle.REQUESTED,
                new WithdrawalAuditEvent(id, WithdrawalAuditEventType.REQUEST_CREATED, Instant.EPOCH));
    }
}
