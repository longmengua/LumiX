package com.lumix.ledger.application.posting;

import java.util.List;
import java.util.Objects;

/**
 * ledger posting command 被拒絕時的安全原因。
 *
 * 這個結構只回傳可對外揭露的安全資訊，不得包含 SQL、stack trace 或 secret。
 */
public record LedgerPostingRejection(
        String reasonCode,
        String message,
        List<String> safeDetails
) {

    public LedgerPostingRejection {
        // 拒絕原因必須能幫助維運判讀，但不能把底層實作細節外洩出去。
        Objects.requireNonNull(reasonCode, "reasonCode must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(safeDetails, "safeDetails must not be null");
        reasonCode = reasonCode.trim();
        message = message.trim();
        safeDetails = List.copyOf(safeDetails);
        if (reasonCode.isEmpty()) {
            throw new IllegalArgumentException("reasonCode must not be blank");
        }
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
