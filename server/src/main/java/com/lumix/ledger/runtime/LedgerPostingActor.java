package com.lumix.ledger.runtime;

import java.util.Objects;

/**
 * ledger 入帳的已驗證內部行為者。
 *
 * <p>這不是 browser 可提交的身分宣告；呼叫端必須先完成自己的 authentication / authorization，
 * 再把可稽核的 actor 放進內部 command。保留 actor 可讓 append-only journal 與 audit evidence
 * 在同一個 transaction 內關聯，避免事後無法追查資金來源。</p>
 */
public record LedgerPostingActor(String actorType, String actorId) {

    public LedgerPostingActor {
        actorType = requireAllowedActorType(actorType);
        actorId = requireText(actorId, "actorId");
    }

    private static String requireAllowedActorType(String value) {
        String normalized = requireText(value, "actorType");
        if (!normalized.equals("USER") && !normalized.equals("ADMIN")
                && !normalized.equals("SYSTEM") && !normalized.equals("SERVICE")) {
            throw new IllegalArgumentException("actorType must be a supported audit actor type");
        }
        return normalized;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
