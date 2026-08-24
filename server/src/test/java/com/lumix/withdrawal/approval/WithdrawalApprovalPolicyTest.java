package com.lumix.withdrawal.approval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
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
import java.util.Set;
import org.junit.jupiter.api.Test;

class WithdrawalApprovalPolicyTest {

    private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");
    private final WithdrawalApprovalPolicy policy = new WithdrawalApprovalPolicy();

    @Test
    void twoDistinctRequiredRolesApproveOnlyAReadyBoundedRequest() {
        // 不同角色、不同 reviewer 的證據齊全時才可得到純 approval outcome，且 outcome 仍不是簽章命令。
        WithdrawalApprovalResult result = policy.evaluate(input("10"), List.of(
                evidence("reviewer-a", WithdrawalApprovalRole.REVIEWER),
                evidence("reviewer-b", WithdrawalApprovalRole.SENIOR_REVIEWER)), requirement("10"), NOW);

        assertEquals(WithdrawalApprovalDecision.APPROVED, result.decision());
        assertTrue(result.isApproved());
    }

    @Test
    void ownerDuplicateReviewerMissingRoleAmountAndExpiryAllFailClosed() {
        // 任何職責分離、限額或時效缺口均不可藉由重送審核資料取得通過結果。
        assertEquals(WithdrawalApprovalDecision.REQUEST_OWNER_REJECTED,
                policy.evaluate(input("10"), List.of(evidence("owner", WithdrawalApprovalRole.REVIEWER)), requirement("10"), NOW).decision());
        assertEquals(WithdrawalApprovalDecision.DUPLICATE_REVIEWER_REJECTED,
                policy.evaluate(input("10"), List.of(evidence("reviewer-a", WithdrawalApprovalRole.REVIEWER), evidence("reviewer-a", WithdrawalApprovalRole.SENIOR_REVIEWER)), requirement("10"), NOW).decision());
        assertEquals(WithdrawalApprovalDecision.MISSING_REQUIRED_ROLE_REJECTED,
                policy.evaluate(input("10"), List.of(evidence("reviewer-a", WithdrawalApprovalRole.REVIEWER)), requirement("10"), NOW).decision());
        assertEquals(WithdrawalApprovalDecision.AMOUNT_LIMIT_REJECTED,
                policy.evaluate(input("11"), List.of(evidence("reviewer-a", WithdrawalApprovalRole.REVIEWER), evidence("reviewer-b", WithdrawalApprovalRole.SENIOR_REVIEWER)), requirement("10"), NOW).decision());
        assertEquals(WithdrawalApprovalDecision.APPROVAL_EXPIRED_REJECTED,
                policy.evaluate(input("10"), List.of(evidence("reviewer-a", WithdrawalApprovalRole.REVIEWER), evidence("reviewer-b", WithdrawalApprovalRole.SENIOR_REVIEWER)), requirement("10", NOW), NOW).decision());
    }

    private static WithdrawalApprovalEvidence evidence(String reviewer, WithdrawalApprovalRole role) {
        return new WithdrawalApprovalEvidence(new UserId(reviewer), role, "authorization-v1", NOW.minusSeconds(1));
    }

    private static WithdrawalApprovalRequirement requirement(String maximumAmount) {
        return requirement(maximumAmount, NOW.plusSeconds(60));
    }

    private static WithdrawalApprovalRequirement requirement(String maximumAmount, Instant expiresAt) {
        return new WithdrawalApprovalRequirement(Set.of(WithdrawalApprovalRole.REVIEWER, WithdrawalApprovalRole.SENIOR_REVIEWER), new WithdrawalAtomicAmount(new BigInteger(maximumAmount)), expiresAt);
    }

    private static P25WithdrawalSignerInput input(String amount) {
        WithdrawalRequestId requestId = new WithdrawalRequestId("request-1");
        WithdrawalNetwork network = new WithdrawalNetwork("ETH_MAINNET", WithdrawalDestinationFormat.EVM_HEX);
        WithdrawalRequest request = new WithdrawalRequest(requestId, new UserId("owner"), new AssetSymbol("USDT"), network,
                WithdrawalDestination.from("0xabcdef0000000000000000000000000000000000", network), new WithdrawalAtomicAmount(new BigInteger(amount)),
                new WithdrawalRequestIdempotencyKey("withdraw-1"), WithdrawalRequestLifecycle.REQUESTED,
                new WithdrawalAuditEvent(requestId, WithdrawalAuditEventType.REQUEST_CREATED, Instant.EPOCH));
        WithdrawalRequestState state = new WithdrawalRequestState(request, WithdrawalRequestLifecycle.APPROVAL_HANDOFF_READY,
                List.of(request.createdAuditEvent()));
        return new P25WithdrawalSignerInput(state, new WithdrawalHoldEvidence(requestId, "hold-1"), "0".repeat(64));
    }
}
