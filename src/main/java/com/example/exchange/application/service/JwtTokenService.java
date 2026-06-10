/*
 * File purpose: Issue HS256 JWTs compatible with the existing JwtAuthenticator.
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.AppUserRecord;
import com.example.exchange.infra.config.ApiAuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtTokenService {

    // Access tokens are intentionally short-lived; logout revokes refresh sessions rather than issued JWTs.
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
    // Local fallback keeps dev registration usable; production must inject API_AUTH_JWT_HMAC_SECRET.
    private static final String DEV_FALLBACK_SECRET = "dev-local-auth-secret-change-me";

    private final ApiAuthProperties apiAuthProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public JwtTokenService(ApiAuthProperties apiAuthProperties, ObjectMapper objectMapper) {
        this(apiAuthProperties, objectMapper, Clock.systemUTC());
    }

    JwtTokenService(ApiAuthProperties apiAuthProperties, ObjectMapper objectMapper, Clock clock) {
        this.apiAuthProperties = apiAuthProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** Issues an HS256 JWT whose roles/scope shape matches JwtAuthenticator and ApiAuthenticationInterceptor. */
    public IssuedToken issueAccessToken(AppUserRecord user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(ACCESS_TOKEN_TTL);
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", String.valueOf(user.getId()));
        claims.put("email", user.getEmail());
        claims.put("roles", user.getRoles());
        claims.put("scope", user.getScopes());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        String signingInput = base64Json(header) + "." + base64Json(claims);
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(hmacSha256(signingInput, effectiveSecret()));
        return new IssuedToken(signingInput + "." + signature, expiresAt);
    }

    /** Encodes JSON deterministically enough for signing while keeping JWT creation dependency-light. */
    private String base64Json(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("jwt json encoding failed", ex);
        }
    }

    /** Shared by token issuing and /me verification so dev fallback behavior is consistent. */
    public String effectiveSecret() {
        String configured = apiAuthProperties.getJwtHmacSecret();
        return configured == null || configured.isBlank() ? DEV_FALLBACK_SECRET : configured;
    }

    /** Computes the compact-JWT signature with the same algorithm accepted by JwtAuthenticator. */
    private static byte[] hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("HmacSHA256 unavailable", ex);
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
