package com.lumix.user.auth.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Cookie 中的 session 識別與高熵秘密值。
 *
 * <p>只有 {@link #toCookieValue()} 的結果會短暫出現在 HttpOnly Cookie；資料庫僅保存 secretDigest。
 * 因此此型別不可序列化、不可記錄到 log，也不可回傳 API response。</p>
 */
public record SessionSecret(UUID sessionId, String secret, String secretDigest) {

    public SessionSecret {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(secret, "secret must not be null");
        Objects.requireNonNull(secretDigest, "secretDigest must not be null");
    }

    /** 組合為 cookie 值；分隔符不可出現在 UUID 或 URL-safe Base64 secret 中。 */
    public String toCookieValue() {
        return sessionId + "." + secret;
    }
}
