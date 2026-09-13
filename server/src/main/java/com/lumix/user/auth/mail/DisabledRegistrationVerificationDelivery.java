package com.lumix.user.auth.mail;

import com.lumix.user.auth.application.RegistrationVerificationDeliveryPort;
import com.lumix.user.auth.domain.RegistrationVerificationSecret;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 未設定受控 SMTP 時的 fail-closed adapter，不能建立使用者卻宣稱已完成 email 驗證。 */
@Component
@Profile("infrastructure")
@ConditionalOnProperty(prefix = "lumix.auth.password-reset", name = "smtp-enabled", havingValue = "false", matchIfMissing = true)
public class DisabledRegistrationVerificationDelivery implements RegistrationVerificationDeliveryPort {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void deliver(String email, RegistrationVerificationSecret secret) {
        throw new IllegalStateException("Registration verification SMTP delivery is disabled");
    }
}
