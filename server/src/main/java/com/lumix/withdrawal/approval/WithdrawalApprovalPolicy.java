package com.lumix.withdrawal.approval;

import com.lumix.account.UserId;
import com.lumix.withdrawal.request.WithdrawalRequestLifecycle;
import com.lumix.withdrawal.request.reconciliation.P25WithdrawalSignerInput;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * P25-T01 的 deterministic 審核規則。
 *
 * 此類別只消費 P24 已對帳的 keyless input 與外部 authorization evidence；它不驗證帳號權限、
 * 不寫入審核結果，且不接觸 signer、secret 或鏈上系統。
 */
public final class WithdrawalApprovalPolicy {

    /**
     * 將職責分離、限額與期限在任何可簽章資料出現前 fail-closed 檢查完畢。
     */
    public WithdrawalApprovalResult evaluate(
            P25WithdrawalSignerInput signerInput,
            List<WithdrawalApprovalEvidence> evidence,
            WithdrawalApprovalRequirement requirement,
            Instant evaluatedAt
    ) {
        signerInput = Objects.requireNonNull(signerInput, "signerInput");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
        requirement = Objects.requireNonNull(requirement, "requirement");
        evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt");

        if (signerInput.requestState().lifecycle() != WithdrawalRequestLifecycle.APPROVAL_HANDOFF_READY
                || !signerInput.requestState().request().requestId().equals(signerInput.holdEvidence().requestId())) {
            return rejected(WithdrawalApprovalDecision.SIGNER_INPUT_INVALID_REJECTED, evidence);
        }
        boolean hasExpiredEvidence = false;
        for (WithdrawalApprovalEvidence item : evidence) {
            if (!item.approvedAt().isBefore(requirement.expiresAt())) {
                hasExpiredEvidence = true;
                break;
            }
        }
        if (!evaluatedAt.isBefore(requirement.expiresAt()) || hasExpiredEvidence) {
            return rejected(WithdrawalApprovalDecision.APPROVAL_EXPIRED_REJECTED, evidence);
        }
        if (signerInput.requestState().request().amount().atoms().compareTo(requirement.maximumAtomicAmount().atoms()) > 0) {
            return rejected(WithdrawalApprovalDecision.AMOUNT_LIMIT_REJECTED, evidence);
        }

        UserId requestOwner = signerInput.requestState().request().ownerUserId();
        if (evidence.stream().anyMatch(item -> requestOwner.equals(item.reviewerUserId()))) {
            return rejected(WithdrawalApprovalDecision.REQUEST_OWNER_REJECTED, evidence);
        }

        Set<UserId> distinctReviewers = new HashSet<>();
        if (evidence.stream().anyMatch(item -> !distinctReviewers.add(item.reviewerUserId()))) {
            return rejected(WithdrawalApprovalDecision.DUPLICATE_REVIEWER_REJECTED, evidence);
        }

        Set<WithdrawalApprovalRole> suppliedRoles = evidence.stream().map(WithdrawalApprovalEvidence::role).collect(java.util.stream.Collectors.toSet());
        if (!suppliedRoles.containsAll(requirement.requiredRoles())) {
            return rejected(WithdrawalApprovalDecision.MISSING_REQUIRED_ROLE_REJECTED, evidence);
        }
        return new WithdrawalApprovalResult(WithdrawalApprovalDecision.APPROVED, evidence);
    }

    private static WithdrawalApprovalResult rejected(WithdrawalApprovalDecision decision, List<WithdrawalApprovalEvidence> evidence) {
        return new WithdrawalApprovalResult(decision, evidence);
    }
}
