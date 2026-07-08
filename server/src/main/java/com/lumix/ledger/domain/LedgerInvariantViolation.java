package com.lumix.ledger.domain;

import java.util.Objects;

/**
 * ledger invariant 失敗原因。
 *
 * 這個 violation 只描述 domain contract 的失敗，不攜帶任何 persistence 或 runtime 狀態。
 */
public record LedgerInvariantViolation(
        String ruleCode,
        String message
) {

    public LedgerInvariantViolation {
        // rule code 與 message 都是後續除錯與審核的重要線索，不能留空。
        Objects.requireNonNull(ruleCode, "ruleCode must not be null");
        Objects.requireNonNull(message, "message must not be null");
        ruleCode = ruleCode.trim();
        message = message.trim();
        if (ruleCode.isEmpty()) {
            throw new IllegalArgumentException("ruleCode must not be blank");
        }
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
