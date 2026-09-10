package com.lumix.user.auth.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * 個人中心可安全呈現的使用者資料投影。
 *
 * <p>此型別刻意只保留帳戶本人可讀的基本資料；它不是 KYC、資產、權限或安全事件的聚合模型，避免
 * 個人中心的 profile API 被誤用為敏感帳戶資訊入口。</p>
 */
public record UserProfile(String userId, String email, String displayName, Instant createdAt) {

    public UserProfile {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
