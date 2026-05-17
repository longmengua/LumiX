/*
 * 檔案用途：測試 API key hash 驗證與角色、scope 解析。
 */
package com.example.exchange.interfaces.web.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyAuthenticatorTest {

    @Test
    void authenticatesSha256HashedApiKey() {
        String rawApiKey = "prod-secret-key";
        String configuredKeys =
                "ops:" + ApiKeyAuthenticator.sha256Hex(rawApiKey) + ":ROLE_ADMIN|ROLE_TRADER:admin|trade:write";

        ApiKeyAuthenticator authenticator =
                new ApiKeyAuthenticator(configuredKeys);

        ApiPrincipal principal =
                authenticator.authenticate(rawApiKey).orElseThrow();

        assertThat(principal.subject()).isEqualTo("ops");
        assertThat(principal.credentialType()).isEqualTo("API_KEY");
        assertThat(principal.roles()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_TRADER");
        assertThat(principal.scopes()).containsExactlyInAnyOrder("admin", "trade:write");
    }

    @Test
    void rejectsUnknownApiKey() {
        String configuredKeys =
                "ops:" + ApiKeyAuthenticator.sha256Hex("expected") + ":ROLE_ADMIN:admin";

        ApiKeyAuthenticator authenticator =
                new ApiKeyAuthenticator(configuredKeys);

        assertThat(authenticator.authenticate("actual")).isEmpty();
    }
}
