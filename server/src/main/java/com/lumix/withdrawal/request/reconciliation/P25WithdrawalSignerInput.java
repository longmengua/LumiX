package com.lumix.withdrawal.request.reconciliation;

import com.lumix.withdrawal.request.lifecycle.WithdrawalRequestState;
import java.util.Objects;
/** P25 可審查的 immutable input；刻意沒有私鑰、簽名、交易 payload 或 broadcast 指令。 */
public record P25WithdrawalSignerInput(WithdrawalRequestState requestState, WithdrawalHoldEvidence holdEvidence, String auditDigest) {
    public P25WithdrawalSignerInput { requestState = Objects.requireNonNull(requestState, "requestState"); holdEvidence = Objects.requireNonNull(holdEvidence, "holdEvidence"); auditDigest = Objects.requireNonNull(auditDigest, "auditDigest"); if (!auditDigest.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("audit digest must be SHA-256 hex"); }
}
