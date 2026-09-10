package com.lumix.user.auth.mail;

import com.lumix.user.auth.application.LoginVerificationDeliveryPort;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import com.lumix.user.auth.domain.LoginVerificationSecret;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 未配置既有受控 SMTP 時拒絕新裝置登入，不能讓使用者卡在無法收到確認信的半成品流程。 */
@Component
@Profile("infrastructure")
@ConditionalOnProperty(prefix = "lumix.auth.password-reset", name = "smtp-enabled", havingValue = "false", matchIfMissing = true)
public class DisabledLoginVerificationDelivery implements LoginVerificationDeliveryPort {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void deliver(AuthenticatedUser user, LoginRequestMetadata metadata, LoginVerificationSecret secret) {
        throw new IllegalStateException("Login verification SMTP delivery is disabled");
    }
}
