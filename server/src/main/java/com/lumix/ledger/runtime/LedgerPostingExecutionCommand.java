package com.lumix.ledger.runtime;

import com.lumix.ledger.application.posting.LedgerPostingCommand;

import java.util.Objects;

/**
 * 可被內部 runtime 執行的 ledger posting command。
 *
 * <p>此型別刻意沒有 HTTP DTO，也不允許指定 balance projection 或 reservation；它只把已通過
 * 上游權限審核的 actor、不可變 journal draft 與 idempotency key 綁在一起。</p>
 */
public record LedgerPostingExecutionCommand(
        LedgerPostingCommand postingCommand,
        String idempotencyKey,
        LedgerPostingActor actor
) {

    public LedgerPostingExecutionCommand {
        Objects.requireNonNull(postingCommand, "postingCommand must not be null");
        idempotencyKey = requireIdempotencyKey(idempotencyKey);
        Objects.requireNonNull(actor, "actor must not be null");
    }

    private static String requireIdempotencyKey(String value) {
        Objects.requireNonNull(value, "idempotencyKey must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 128) {
            throw new IllegalArgumentException("idempotencyKey must contain 1 to 128 characters");
        }
        return normalized;
    }
}
