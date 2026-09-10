package com.lumix.user.auth.application;

import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import com.lumix.user.auth.domain.LoginVerificationSecret;

/**
 * 新裝置登入確認的寄送邊界。
 *
 * <p>application layer 只交付一次性 email secret 與去敏登入快照；adapter 不得記錄 token、完整 URL、
 * 密碼、session 或 pending cookie。</p>
 */
public interface LoginVerificationDeliveryPort {

    boolean isAvailable();

    void deliver(AuthenticatedUser user, LoginRequestMetadata metadata, LoginVerificationSecret secret);
}
