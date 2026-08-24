package com.lumix.withdrawal.request.reconciliation;

import com.lumix.withdrawal.request.WithdrawalAuditEventType;
import com.lumix.withdrawal.request.WithdrawalRequestLifecycle;
import com.lumix.withdrawal.request.lifecycle.WithdrawalRequestState;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** P24-T04 的 pure reconciliation；只在 approval handoff state、有效 audit 與 matching hold evidence 齊全時交給 P25。 */
public final class WithdrawalRequestReconciliationPolicy {
    public WithdrawalRequestReconciliationReport evaluate(WithdrawalRequestState state, Optional<WithdrawalHoldEvidence> holdEvidence) {
        state = Objects.requireNonNull(state, "state"); holdEvidence = Objects.requireNonNull(holdEvidence, "holdEvidence");
        java.util.ArrayList<WithdrawalReconciliationException> exceptions = new java.util.ArrayList<>();
        if (state.lifecycle() != WithdrawalRequestLifecycle.APPROVAL_HANDOFF_READY) exceptions.add(WithdrawalReconciliationException.NOT_READY_FOR_SIGNER);
        if (state.auditEvents().stream().noneMatch(event -> event.type() == WithdrawalAuditEventType.APPROVAL_HANDOFF_RECORDED)) exceptions.add(WithdrawalReconciliationException.INVALID_AUDIT_TRAIL);
        if (holdEvidence.isEmpty() || !holdEvidence.orElseThrow().requestId().equals(state.request().requestId())) exceptions.add(WithdrawalReconciliationException.MISSING_HOLD_EVIDENCE);
        if (!exceptions.isEmpty()) return new WithdrawalRequestReconciliationReport(exceptions, Optional.empty());
        return new WithdrawalRequestReconciliationReport(List.of(), Optional.of(new P25WithdrawalSignerInput(state, holdEvidence.orElseThrow(), digest(state))));
    }
    private static String digest(WithdrawalRequestState state) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(state.toString().getBytes(StandardCharsets.UTF_8))); } catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 must exist", exception); } }
}
