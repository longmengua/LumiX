package com.lumix.withdrawal.request.lifecycle;

import com.lumix.withdrawal.request.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * P24-T03 的 optimistic, immutable request state transition policy。
 *
 * <p>expected lifecycle 是 race 防線；approval handoff 是不可逆的資料狀態，不代表任何管理員核准、簽章或廣播。</p>
 */
public final class WithdrawalRequestTransitionPolicy {
    public WithdrawalRequestTransitionResult evaluate(WithdrawalRequestState state, WithdrawalRequestLifecycle expected, WithdrawalRequestAction action, Instant occurredAt) {
        state = Objects.requireNonNull(state, "state must not be null"); expected = Objects.requireNonNull(expected, "expected must not be null");
        action = Objects.requireNonNull(action, "action must not be null"); occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (state.lifecycle() != expected) return new WithdrawalRequestTransitionResult(WithdrawalRequestTransitionDecision.STALE_STATE_REJECTED, state);
        if (alreadyTerminalForAction(state.lifecycle(), action)) return new WithdrawalRequestTransitionResult(WithdrawalRequestTransitionDecision.IDEMPOTENT_NO_CHANGE, state);
        WithdrawalRequestLifecycle next = next(state.lifecycle(), action);
        if (next == null) return new WithdrawalRequestTransitionResult(WithdrawalRequestTransitionDecision.INVALID_TRANSITION_REJECTED, state);
        List<WithdrawalAuditEvent> events = new ArrayList<>(state.auditEvents());
        events.add(new WithdrawalAuditEvent(state.request().requestId(), eventType(action), occurredAt));
        return new WithdrawalRequestTransitionResult(WithdrawalRequestTransitionDecision.TRANSITIONED, new WithdrawalRequestState(state.request(), next, events));
    }
    private static boolean alreadyTerminalForAction(WithdrawalRequestLifecycle lifecycle, WithdrawalRequestAction action) {
        return (action == WithdrawalRequestAction.CANCEL && lifecycle == WithdrawalRequestLifecycle.CANCELLED)
                || (action == WithdrawalRequestAction.EXPIRE && lifecycle == WithdrawalRequestLifecycle.EXPIRED);
    }
    private static WithdrawalRequestLifecycle next(WithdrawalRequestLifecycle state, WithdrawalRequestAction action) {
        return switch (action) {
            case CANCEL -> switch (state) { case REQUESTED, ELIGIBILITY_PENDING, MANUAL_REVIEW_PENDING -> WithdrawalRequestLifecycle.CANCELLED; default -> null; };
            case EXPIRE -> switch (state) { case REQUESTED, ELIGIBILITY_PENDING, MANUAL_REVIEW_PENDING -> WithdrawalRequestLifecycle.EXPIRED; default -> null; };
            case QUEUE_MANUAL_REVIEW -> switch (state) { case REQUESTED, ELIGIBILITY_PENDING -> WithdrawalRequestLifecycle.MANUAL_REVIEW_PENDING; default -> null; };
            case PREPARE_APPROVAL_HANDOFF -> state == WithdrawalRequestLifecycle.MANUAL_REVIEW_PENDING ? WithdrawalRequestLifecycle.APPROVAL_HANDOFF_READY : null;
        };
    }
    private static WithdrawalAuditEventType eventType(WithdrawalRequestAction action) {
        return switch (action) { case CANCEL -> WithdrawalAuditEventType.CANCELLATION_RECORDED; case EXPIRE -> WithdrawalAuditEventType.EXPIRATION_RECORDED; case QUEUE_MANUAL_REVIEW -> WithdrawalAuditEventType.MANUAL_REVIEW_QUEUED; case PREPARE_APPROVAL_HANDOFF -> WithdrawalAuditEventType.APPROVAL_HANDOFF_RECORDED; };
    }
}
