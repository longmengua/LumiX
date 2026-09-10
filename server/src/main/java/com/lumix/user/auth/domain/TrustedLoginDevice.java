package com.lumix.user.auth.domain;

import java.util.Objects;
import java.util.UUID;

/** 已驗證裝置的最小安全投影；不包含原始裝置 cookie 秘密。 */
public record TrustedLoginDevice(UUID deviceId, String userAgentDigest) {

    public TrustedLoginDevice {
        Objects.requireNonNull(deviceId, "deviceId must not be null");
        Objects.requireNonNull(userAgentDigest, "userAgentDigest must not be null");
    }
}
