package com.lumix.user.auth.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * 受信任裝置 cookie 的識別與高熵秘密值。
 *
 * <p>原始值只可短暫放在 HttpOnly cookie；持久化層只能接收 {@code secretDigest}。此型別不可作為 API
 * response、log 欄位或一般序列化 payload，避免裝置 cookie 成為可重放的認證材料。</p>
 */
public record DeviceSecret(UUID deviceId, String secret, String secretDigest) {

    public DeviceSecret {
        Objects.requireNonNull(deviceId, "deviceId must not be null");
        Objects.requireNonNull(secret, "secret must not be null");
        Objects.requireNonNull(secretDigest, "secretDigest must not be null");
    }

    /** 裝置 cookie 格式固定，UUID 與 URL-safe Base64 秘密值不會與分隔符衝突。 */
    public String toCookieValue() {
        return deviceId + "." + secret;
    }
}
