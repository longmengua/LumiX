package com.lumix.user.auth.config;

import com.lumix.infrastructure.security.ApiSecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.Assert;

/** 將密碼雜湊器集中於認證邊界，避免業務程式自行選擇或弱化演算法。 */
@Configuration
@Profile("infrastructure")
public class UserAuthenticationConfiguration {

    @Bean
    BCryptPasswordEncoder passwordEncoder(
        UserAuthenticationProperties properties,
        ApiSecurityProperties securityProperties
    ) {
        // BCrypt cost 過低會降低暴力破解成本，過高則可能形成可用性風險，因此限制可審核範圍。
        Assert.isTrue(properties.getBcryptStrength() >= 10 && properties.getBcryptStrength() <= 16,
            "lumix.auth.bcryptStrength must be between 10 and 16");
        Assert.hasText(properties.getCookieName(), "lumix.auth.cookieName is required");
        Assert.hasText(properties.getDeviceCookieName(), "lumix.auth.deviceCookieName is required");
        Assert.hasText(properties.getLoginVerificationCookieName(), "lumix.auth.loginVerificationCookieName is required");
        Assert.isTrue(!securityProperties.isRequireHttps() || properties.isCookieSecure(),
            "lumix.auth.cookieSecure must be true when lumix.security.requireHttps is enabled");
        Assert.isTrue(!properties.getSessionTtl().isNegative() && !properties.getSessionTtl().isZero(),
            "lumix.auth.sessionTtl must be positive");
        Assert.isTrue(!properties.getDeviceTtl().isNegative() && !properties.getDeviceTtl().isZero(),
            "lumix.auth.deviceTtl must be positive");
        Assert.isTrue(!properties.getPasswordReset().getTtl().isNegative()
                && !properties.getPasswordReset().getTtl().isZero(),
            "lumix.auth.passwordReset.ttl must be positive");
        Assert.isTrue(!properties.getLoginVerification().getTtl().isNegative()
                && !properties.getLoginVerification().getTtl().isZero(),
            "lumix.auth.loginVerification.ttl must be positive");
        Assert.isTrue(!properties.getRegistrationVerification().getTtl().isNegative()
                && !properties.getRegistrationVerification().getTtl().isZero(),
            "lumix.auth.registrationVerification.ttl must be positive");
        Assert.isTrue(properties.getRegistrationVerification().getMaxAttempts() >= 1
                && properties.getRegistrationVerification().getMaxAttempts() <= 10,
            "lumix.auth.registrationVerification.maxAttempts must be between 1 and 10");
        Assert.isTrue(properties.getRegistrationVerification().getLetterOptionCount() >= 3
                && properties.getRegistrationVerification().getLetterOptionCount() <= 5,
            "lumix.auth.registrationVerification.letterOptionCount must be between 3 and 5");
        return new BCryptPasswordEncoder(properties.getBcryptStrength());
    }
}
