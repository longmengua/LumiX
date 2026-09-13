package com.lumix.user.auth.domain;

import java.util.Objects;

/**
 * 本次登入嘗試的去敏安全快照。
 *
 * <p>只保存供本人通知與歷程顯示的 IP、友善裝置標籤與 User-Agent 摘要；不可把完整 User-Agent 或任何
 * session／cookie 秘密保存到登入歷程。</p>
 */
public record LoginRequestMetadata(
    String ipAddress,
    String deviceLabel,
    String userAgentDigest,
    BoundDevicePlatform devicePlatform
) {

    /** 舊測試與內部呼叫未提供類別時採保守桌電槽位，避免未知類別繞過有限裝置規則。 */
    public LoginRequestMetadata(String ipAddress, String deviceLabel, String userAgentDigest) {
        this(ipAddress, deviceLabel, userAgentDigest, BoundDevicePlatform.DESKTOP);
    }

    public LoginRequestMetadata {
        ipAddress = requireBounded(ipAddress, "ipAddress", 64);
        deviceLabel = requireBounded(deviceLabel, "deviceLabel", 256);
        userAgentDigest = requireBounded(userAgentDigest, "userAgentDigest", 64);
        devicePlatform = Objects.requireNonNull(devicePlatform, "devicePlatform must not be null");
    }

    private static String requireBounded(String value, String name, int maximumLength) {
        String normalized = Objects.requireNonNull(value, name + " must not be null").trim();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new IllegalArgumentException(name + " must be bounded text");
        }
        return normalized;
    }
}
