package com.lumix.user.auth.application;

import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.PasswordResetSecret;

/**
 * 密碼重設通知的輸出邊界。
 *
 * <p>application layer 只交付一次性秘密值，不知道 SMTP、供應商或寄信佇列細節。任何 adapter
 * 都不得將 token 記錄到 log、監控 tag 或例外訊息。</p>
 */
public interface PasswordResetDeliveryPort {

    boolean isAvailable();

    void deliver(AuthenticatedUser user, PasswordResetSecret secret);
}
