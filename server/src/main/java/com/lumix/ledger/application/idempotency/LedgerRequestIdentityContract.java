package com.lumix.ledger.application.idempotency;

import com.lumix.common.RequestId;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;

import java.util.Objects;

/**
 * ledger request identity 的設計契約。
 *
 * requestId 只做 trace / correlation / audit linkage；
 * idempotencyKey 才負責 duplicate prevention contract；
 * businessReferenceType 與 businessReferenceId 只描述業務來源，不可單獨取代 idempotencyKey。
 */
public record LedgerRequestIdentityContract(
        RequestId requestId,
        LedgerIdempotencyScope scope,
        String idempotencyKey,
        LedgerBusinessReferenceType businessReferenceType,
        String businessReferenceId
) {

    public LedgerRequestIdentityContract {
        // 這份契約的欄位是設計輸出的一部分，不能留下空白或模糊值，避免後續把 trace 與去重語意混在一起。
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(businessReferenceType, "businessReferenceType must not be null");
        Objects.requireNonNull(businessReferenceId, "businessReferenceId must not be null");

        idempotencyKey = idempotencyKey.trim();
        businessReferenceId = businessReferenceId.trim();
        if (idempotencyKey.isEmpty()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        if (businessReferenceId.isEmpty()) {
            throw new IllegalArgumentException("businessReferenceId must not be blank");
        }
    }
}
