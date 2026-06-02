/*
 * 檔案用途：測試 private user stream 訂閱授權規則。
 */
package com.example.exchange.interfaces.web.security;

import com.example.exchange.infra.config.ApiAuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class UserStreamSubscriptionAuthorizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("auth 關閉時 private user stream 暫時放行")
    void allowsWhenAuthDisabled() {
        ApiAuthProperties properties = new ApiAuthProperties();
        properties.setEnabled(false);
        UserStreamSubscriptionAuthorizer authorizer =
                new UserStreamSubscriptionAuthorizer(properties, objectMapper);

        // 場景：dev profile 未啟用 API auth 時，不阻斷既有本機 SSE/WebSocket 測試流。
        UserStreamSubscriptionAuthorizer.UserStreamAuthorizationDecision decision =
                authorizer.authorize(42L, null, null);

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    @DisplayName("auth 開啟時 private user stream 缺 credentials 會回 401")
    void rejectsMissingCredentialsWhenAuthEnabled() {
        UserStreamSubscriptionAuthorizer authorizer =
                new UserStreamSubscriptionAuthorizer(enabledProperties("42", "ROLE_USER", "stream:read", "user-key"), objectMapper);

        // 場景：private user stream 不能在 production auth 開啟時匿名訂閱。
        UserStreamSubscriptionAuthorizer.UserStreamAuthorizationDecision decision =
                authorizer.authorize(42L, null, null);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(decision.reason()).isEqualTo("USER_STREAM_AUTH_REQUIRED");
    }

    @Test
    @DisplayName("同 uid 且有 stream read scope 的 principal 可訂閱自己的 user stream")
    void allowsOwnerWithStreamReadScope() {
        UserStreamSubscriptionAuthorizer authorizer =
                new UserStreamSubscriptionAuthorizer(enabledProperties("user-42", "ROLE_USER", "stream:read", "user-key"), objectMapper);

        // 場景：使用者只能用可讀 stream scope 訂閱自己的 uid channel。
        UserStreamSubscriptionAuthorizer.UserStreamAuthorizationDecision decision =
                authorizer.authorize(42L, "user-key", null);

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    @DisplayName("不同 uid 的 user stream 訂閱會回 403")
    void rejectsDifferentUidSubscription() {
        UserStreamSubscriptionAuthorizer authorizer =
                new UserStreamSubscriptionAuthorizer(enabledProperties("user-42", "ROLE_USER", "stream:read", "user-key"), objectMapper);

        // 場景：同一個 user credential 嘗試訂閱別人的 private stream，必須被擋下。
        UserStreamSubscriptionAuthorizer.UserStreamAuthorizationDecision decision =
                authorizer.authorize(43L, "user-key", null);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(decision.reason()).isEqualTo("USER_STREAM_PERMISSION_DENIED");
    }

    @Test
    @DisplayName("admin principal 可訂閱任意 user stream 供營運排障")
    void allowsAdminForAnyUserStream() {
        UserStreamSubscriptionAuthorizer authorizer =
                new UserStreamSubscriptionAuthorizer(enabledProperties("ops", "ROLE_ADMIN", "admin", "admin-key"), objectMapper);

        // 場景：營運管理員可用 admin role/scope 訂閱任意 uid stream 進行排障。
        UserStreamSubscriptionAuthorizer.UserStreamAuthorizationDecision decision =
                authorizer.authorize(42L, "admin-key", null);

        assertThat(decision.allowed()).isTrue();
    }

    private static ApiAuthProperties enabledProperties(String subject, String roles, String scopes, String rawKey) {
        ApiAuthProperties properties = new ApiAuthProperties();
        properties.setEnabled(true);
        properties.setJwtEnabled(false);
        properties.setApiKeys(subject
                + ":"
                + ApiKeyAuthenticator.sha256Hex(rawKey)
                + ":"
                + roles
                + ":"
                + scopes);
        return properties;
    }
}
