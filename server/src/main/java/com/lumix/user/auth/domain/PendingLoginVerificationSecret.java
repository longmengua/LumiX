package com.lumix.user.auth.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * 原始登入瀏覽器持有的一次性完成秘密。
 *
 * <p>它同時攜帶候選裝置 cookie，核准前仍不是 trusted device。只有資料庫內同一個 pending digest 已標記
 * APPROVED 時，才可把候選裝置寫為可信並建立一個 session。</p>
 */
public record PendingLoginVerificationSecret(
    UUID verificationRequestId,
    String secret,
    String secretDigest,
    DeviceSecret candidateDevice
) {

    public PendingLoginVerificationSecret {
        Objects.requireNonNull(verificationRequestId, "verificationRequestId must not be null");
        Objects.requireNonNull(secret, "secret must not be null");
        Objects.requireNonNull(secretDigest, "secretDigest must not be null");
        Objects.requireNonNull(candidateDevice, "candidateDevice must not be null");
    }

    /** 只有 HttpOnly cookie 使用此格式；不可暴露到 JSON、URL 或 email。 */
    public String toCookieValue() {
        return verificationRequestId + "." + secret + "." + candidateDevice.deviceId() + "." + candidateDevice.secret();
    }
}
