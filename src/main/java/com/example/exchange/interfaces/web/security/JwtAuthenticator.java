/*
 * 檔案用途：Web 安全工具，驗證 HS256 JWT 並解析角色與 scope。
 */
package com.example.exchange.interfaces.web.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JwtAuthenticator {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String hmacSecret;
    private final long clockSkewSeconds;

    public JwtAuthenticator(ObjectMapper objectMapper, String hmacSecret, long clockSkewSeconds) {
        this.objectMapper = objectMapper;
        this.hmacSecret = hmacSecret == null ? "" : hmacSecret;
        this.clockSkewSeconds = clockSkewSeconds;
    }

    public Optional<ApiPrincipal> authenticate(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank() || hmacSecret.isBlank()) {
            return Optional.empty();
        }

        String[] parts = bearerToken.trim().split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        try {
            Map<String, Object> header = decodeJson(parts[0]);
            if (!"HS256".equals(header.get("alg"))) {
                return Optional.empty();
            }

            String signingInput = parts[0] + "." + parts[1];
            byte[] expectedSignature =
                    hmacSha256(signingInput, hmacSecret);
            byte[] actualSignature =
                    Base64.getUrlDecoder().decode(parts[2]);

            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                return Optional.empty();
            }

            Map<String, Object> claims = decodeJson(parts[1]);
            if (!isTimeValid(claims)) {
                return Optional.empty();
            }

            String subject =
                    firstNonBlank(claims.get("sub"), claims.get("client_id"), "unknown");

            return Optional.of(new ApiPrincipal(
                    subject,
                    "JWT",
                    parseValues(claims.get("roles")),
                    parseValues(firstNonNull(claims.get("scope"), claims.get("scopes")))
            ));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private Map<String, Object> decodeJson(String base64Url) {
        try {
            byte[] json =
                    Base64.getUrlDecoder().decode(base64Url);
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid jwt json", e);
        }
    }

    private boolean isTimeValid(Map<String, Object> claims) {
        long now = Instant.now().getEpochSecond();

        Long expiresAt = numericClaim(claims.get("exp"));
        if (expiresAt != null && expiresAt + clockSkewSeconds < now) {
            return false;
        }

        Long notBefore = numericClaim(claims.get("nbf"));
        return notBefore == null || notBefore - clockSkewSeconds <= now;
    }

    private static byte[] hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    private static Long numericClaim(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return null;
    }

    private static Set<String> parseValues(Object value) {
        if (value == null) {
            return Set.of();
        }

        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .collect(Collectors.toUnmodifiableSet());
        }

        return Arrays.stream(value.toString().split("[,\\s]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }

            String text = value.toString();
            if (!text.isBlank()) {
                return text;
            }
        }
        return "unknown";
    }
}
