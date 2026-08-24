package com.lumix.withdrawal.approval;

import com.lumix.account.UserId;
import java.time.Instant;
import java.util.Objects;

/** 一筆 immutable 的審核輸入證據；不宣稱已實際驗證登入身分或授權。 */
public record WithdrawalApprovalEvidence(UserId reviewerUserId, WithdrawalApprovalRole role, String authorizationEvidenceVersion, Instant approvedAt) {
    public WithdrawalApprovalEvidence {
        reviewerUserId = Objects.requireNonNull(reviewerUserId, "reviewerUserId");
        role = Objects.requireNonNull(role, "role");
        authorizationEvidenceVersion = Objects.requireNonNull(authorizationEvidenceVersion, "authorizationEvidenceVersion").trim();
        approvedAt = Objects.requireNonNull(approvedAt, "approvedAt");
        if (authorizationEvidenceVersion.isEmpty()) {
            throw new IllegalArgumentException("authorization evidence version must not be blank");
        }
    }
}
