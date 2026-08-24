package com.lumix.withdrawal.request.lifecycle;

import com.lumix.withdrawal.request.WithdrawalAuditEvent;
import com.lumix.withdrawal.request.WithdrawalRequest;
import com.lumix.withdrawal.request.WithdrawalRequestLifecycle;
import java.util.List;
import java.util.Objects;

/** request 的 immutable state 與完整 audit event history；沒有 hold、approval 或 signer 實作。 */
public record WithdrawalRequestState(WithdrawalRequest request, WithdrawalRequestLifecycle lifecycle, List<WithdrawalAuditEvent> auditEvents) {
    public WithdrawalRequestState {
        request = Objects.requireNonNull(request, "request must not be null");
        lifecycle = Objects.requireNonNull(lifecycle, "lifecycle must not be null");
        auditEvents = List.copyOf(Objects.requireNonNull(auditEvents, "auditEvents must not be null"));
        if (auditEvents.isEmpty() || !request.createdAuditEvent().equals(auditEvents.getFirst())) {
            throw new IllegalArgumentException("state must retain immutable request-created audit event as first evidence");
        }
    }
    public static WithdrawalRequestState created(WithdrawalRequest request) { return new WithdrawalRequestState(request, WithdrawalRequestLifecycle.REQUESTED, List.of(request.createdAuditEvent())); }
}
