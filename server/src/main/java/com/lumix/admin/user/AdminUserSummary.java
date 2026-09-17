package com.lumix.admin.user;

import java.time.Instant;

/** 管理畫面可見的最小使用者摘要；不包含 credential、cookie、token、完整 IP 或 user-agent。 */
public record AdminUserSummary(
    String userId,
    String email,
    String displayName,
    String status,
    Instant createdAt,
    Instant lastLoginAt,
    Instant fundTransferRestrictedUntil,
    Instant withdrawalFrozenAt,
    boolean hasActiveRestriction
) { }
