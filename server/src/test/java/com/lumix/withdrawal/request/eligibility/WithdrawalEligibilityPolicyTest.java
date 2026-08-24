package com.lumix.withdrawal.request.eligibility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.withdrawal.request.*;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class WithdrawalEligibilityPolicyTest {
    private final WithdrawalEligibilityPolicy policy = new WithdrawalEligibilityPolicy();
    private final WithdrawalNetwork network = new WithdrawalNetwork("ETH_MAINNET", WithdrawalDestinationFormat.EVM_HEX);
    private final Instant now = Instant.parse("2026-08-24T00:00:00Z");
    @Test void insufficientExpiredConcurrentAndRiskFailClosed() {
        // 可用餘額、費率有效期、既有 handoff 與風控任一不成立時，都不可產生未來 hold 的資格。
        WithdrawalRequest request = request("request-1", "10");
        WithdrawalAvailableBalanceEvidence balance = new WithdrawalAvailableBalanceEvidence(new UserId("user-a"), new AssetSymbol("USDT"), BigInteger.TEN);
        WithdrawalFeeQuote quote = quote("1", now.plusSeconds(60));
        assertEquals(WithdrawalEligibilityReason.INSUFFICIENT_AVAILABLE_BALANCE, policy.evaluate(request, balance, quote, WithdrawalRiskDecision.ALLOWED, now, List.of()).reason());
        assertEquals(WithdrawalEligibilityReason.FEE_QUOTE_EXPIRED, policy.evaluate(request, balance, quote("1", now), WithdrawalRiskDecision.ALLOWED, now, List.of()).reason());
        WithdrawalHoldHandoff existing = new WithdrawalHoldHandoff(request("request-0", "5"), quote, BigInteger.valueOf(6));
        assertEquals(WithdrawalEligibilityReason.INSUFFICIENT_AVAILABLE_BALANCE, policy.evaluate(request, new WithdrawalAvailableBalanceEvidence(new UserId("user-a"), new AssetSymbol("USDT"), BigInteger.valueOf(16)), quote, WithdrawalRiskDecision.ALLOWED, now, List.of(existing)).reason());
        assertEquals(WithdrawalEligibilityReason.RISK_REJECTED, policy.evaluate(request, balance, quote, WithdrawalRiskDecision.REJECTED, now, List.of()).reason());
    }
    private WithdrawalFeeQuote quote(String fee, Instant expiresAt) { return new WithdrawalFeeQuote("fee-v1", new AssetSymbol("USDT"), network, new WithdrawalAtomicAmount(new BigInteger(fee)), expiresAt); }
    private WithdrawalRequest request(String id, String amount) { WithdrawalRequestId requestId = new WithdrawalRequestId(id); return new WithdrawalRequest(requestId, new UserId("user-a"), new AssetSymbol("USDT"), network, WithdrawalDestination.from("0xabcdef0000000000000000000000000000000000", network), new WithdrawalAtomicAmount(new BigInteger(amount)), new WithdrawalRequestIdempotencyKey(id), WithdrawalRequestLifecycle.REQUESTED, new WithdrawalAuditEvent(requestId, WithdrawalAuditEventType.REQUEST_CREATED, now)); }
}
