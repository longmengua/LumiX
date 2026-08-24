package com.lumix.withdrawal.request;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import java.util.Objects;

/**
 * 尚未批准、簽章或廣播的 immutable withdrawal request。
 */
public record WithdrawalRequest(
        WithdrawalRequestId requestId,
        UserId ownerUserId,
        AssetSymbol asset,
        WithdrawalNetwork network,
        WithdrawalDestination destination,
        WithdrawalAtomicAmount amount,
        WithdrawalRequestIdempotencyKey idempotencyKey,
        WithdrawalRequestLifecycle lifecycle,
        WithdrawalAuditEvent createdAuditEvent
) {

    public WithdrawalRequest {
        requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        ownerUserId = Objects.requireNonNull(ownerUserId, "ownerUserId must not be null");
        asset = Objects.requireNonNull(asset, "asset must not be null");
        network = Objects.requireNonNull(network, "network must not be null");
        destination = Objects.requireNonNull(destination, "destination must not be null");
        amount = Objects.requireNonNull(amount, "amount must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        lifecycle = Objects.requireNonNull(lifecycle, "lifecycle must not be null");
        createdAuditEvent = Objects.requireNonNull(createdAuditEvent, "createdAuditEvent must not be null");
        if (destination.format() != network.destinationFormat()) {
            throw new IllegalArgumentException("withdrawal destination format must match network");
        }
        if (lifecycle != WithdrawalRequestLifecycle.REQUESTED || createdAuditEvent.type() != WithdrawalAuditEventType.REQUEST_CREATED
                || !requestId.equals(createdAuditEvent.requestId())) {
            throw new IllegalArgumentException("new withdrawal request must have matching REQUESTED request-created audit evidence");
        }
    }
}
