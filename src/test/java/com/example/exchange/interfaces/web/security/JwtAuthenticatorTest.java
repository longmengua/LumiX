/*
 * 檔案用途：測試 HS256 JWT 驗證、過期檢查與角色 scope 解析。
 */
package com.example.exchange.interfaces.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
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

    private String jwt(String secret, Map<String, Object> claims) throws Exception {
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

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
