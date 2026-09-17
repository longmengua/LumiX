package com.lumix.admin.user;

import java.time.Instant;

/** 管理端限制命令的最小回應，避免回傳 session、credential 或其他敏感資料。 */
public record AdminUserRestrictionResult(boolean changed, boolean loginFrozen, Instant withdrawalFrozenAt) { }
