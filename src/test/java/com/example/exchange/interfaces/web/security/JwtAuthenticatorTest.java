/*
 * 檔案用途：測試 HS256 JWT 驗證、過期檢查與角色 scope 解析。
 */
package com.example.exchange.interfaces.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆蓋 HS256 JWT authenticator：簽章驗證、roles/scope 轉 principal，以及 exp 過期拒絕。
 */
class JwtAuthenticatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("有效 HS256 JWT 會建立 JWT principal 並解析 roles/scopes")
    /**
     * 流程：用測試 secret 簽有效 token -> authenticate -> 驗證 subject、credential type、roles 與 scopes。
     */
    void authenticatesHs256Jwt() throws Exception {
        String secret = "jwt-test-secret";
        String token = jwt(
                secret,
                Map.of(
                        "sub", "user-1",
                        "roles", List.of("ROLE_TRADER"),
                        "scope", "trade:write funds:write",
                        "exp", Instant.now().plusSeconds(300).getEpochSecond()
                )
        );

        JwtAuthenticator authenticator =
                new JwtAuthenticator(objectMapper, secret, 60);

        ApiPrincipal principal =
                authenticator.authenticate(token).orElseThrow();

        assertThat(principal.subject()).isEqualTo("user-1");
        assertThat(principal.credentialType()).isEqualTo("JWT");
        assertThat(principal.roles()).containsExactly("ROLE_TRADER");
        assertThat(principal.scopes()).containsExactlyInAnyOrder("trade:write", "funds:write");
    }

    @Test
    @DisplayName("exp 已過期的 JWT 會被拒絕")
    /**
     * 流程：建立 exp 在過去的 token -> authenticate -> 驗證過期 token 不產生 principal。
     */
    void rejectsExpiredJwt() throws Exception {
        String secret = "jwt-test-secret";
        String token = jwt(
                secret,
                Map.of(
                        "sub", "user-1",
                        "roles", List.of("ROLE_TRADER"),
                        "exp", Instant.now().minusSeconds(300).getEpochSecond()
                )
        );

        JwtAuthenticator authenticator =
                new JwtAuthenticator(objectMapper, secret, 0);

        assertThat(authenticator.authenticate(token)).isEmpty();
    }

    /**
     * 組出最小 HS256 JWT，讓測試可控制 claims、exp 與簽章 secret。
     */
    private String jwt(String secret, Map<String, Object> claims) throws Exception {
        // 測試直接產生最小 HS256 token，避免把測試行為綁到外部 JWT builder。
        String header =
                base64Url(objectMapper.writeValueAsBytes(Map.of(
                        "alg", "HS256",
                        "typ", "JWT"
                )));
        String payload =
                base64Url(objectMapper.writeValueAsBytes(claims));
        String signingInput =
                header + "." + payload;

        Mac mac =
                Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        return signingInput + "." + base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 將 header/payload/signature 轉成 JWT 需要的 URL-safe base64 且去掉 padding。
     */
    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
