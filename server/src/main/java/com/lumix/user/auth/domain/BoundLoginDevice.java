package com.lumix.user.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 個人中心可讀的受信任裝置投影；刻意排除 cookie 秘密、digest 與完整 User-Agent。 */
public record BoundLoginDevice(
    UUID deviceId,
    String deviceLabel,
    String lastIpAddress,
    Instant createdAt,
    Instant lastSeenAt
) {

    public BoundLoginDevice {
        Objects.requireNonNull(deviceId, "deviceId must not be null");
        Objects.requireNonNull(deviceLabel, "deviceLabel must not be null");
        Objects.requireNonNull(lastIpAddress, "lastIpAddress must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(lastSeenAt, "lastSeenAt must not be null");
    }
}
