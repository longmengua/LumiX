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

    /**
     * 最高管理員首次啟用沿用同一受控的一次性連結傳遞通道，但必須使用可辨識的信件文案。
     *
     * <p>保留 default 實作讓非 SMTP adapter 仍維持既有 fail-closed 行為；不得為了 bootstrap 把 token
     * 改由 log、管理 API 或設定檔送出。</p>
     */
    default void deliverSuperAdminActivation(AuthenticatedUser user, PasswordResetSecret secret) {
        deliver(user, secret);
    }
}
