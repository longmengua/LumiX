package com.lumix.user.auth.mail;

import com.lumix.user.auth.application.PasswordResetDeliveryPort;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.PasswordResetSecret;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 未配置受控 SMTP 時的 fail-closed adapter，禁止用 log 或 API 回傳 token 假裝已寄送。 */
@Component
@Profile("infrastructure")
@ConditionalOnProperty(prefix = "lumix.auth.password-reset", name = "smtp-enabled", havingValue = "false", matchIfMissing = true)
public class DisabledPasswordResetDelivery implements PasswordResetDeliveryPort {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void deliver(AuthenticatedUser user, PasswordResetSecret secret) {
        throw new IllegalStateException("Password reset SMTP delivery is disabled");
    }
}
