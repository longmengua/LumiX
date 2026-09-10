package com.lumix.user.auth.domain;

/** 新裝置登入的明確決定；只有 APPROVED 能由原始登入瀏覽器消耗一次。 */
public enum LoginVerificationState {
    PENDING,
    APPROVED,
    REJECTED
}
