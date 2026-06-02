/*
 * 檔案用途：驗證 private user SSE/WebSocket stream 訂閱權限。
 */
package com.example.exchange.interfaces.web.security;

import com.example.exchange.infra.config.ApiAuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class UserStreamSubscriptionAuthorizer {

    private final ApiAuthProperties properties;
    private final ApiKeyAuthenticator apiKeyAuthenticator;
    private final JwtAuthenticator jwtAuthenticator;

    public UserStreamSubscriptionAuthorizer(ApiAuthProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.apiKeyAuthenticator = new ApiKeyAuthenticator(properties.getApiKeys());
        this.jwtAuthenticator = new JwtAuthenticator(
                objectMapper,
                properties.getJwtHmacSecret(),
                properties.getClockSkewSeconds()
        );
    }

    public UserStreamAuthorizationDecision authorize(long uid, String apiKey, String authorization) {
        if (!properties.isEnabled()) {
            return UserStreamAuthorizationDecision.allow();
        }

        Optional<ApiPrincipal> principal = authenticate(apiKey, authorization);
        if (principal.isEmpty()) {
            return UserStreamAuthorizationDecision.rejected(HttpStatus.UNAUTHORIZED, "USER_STREAM_AUTH_REQUIRED");
        }

        return isAuthorized(principal.get(), uid)
                ? UserStreamAuthorizationDecision.allow()
                : UserStreamAuthorizationDecision.rejected(HttpStatus.FORBIDDEN, "USER_STREAM_PERMISSION_DENIED");
    }

    public String apiKeyHeaderName() {
        return properties.getApiKeyHeader();
    }

    private Optional<ApiPrincipal> authenticate(String apiKey, String authorization) {
        if (properties.isApiKeyEnabled()) {
            Optional<ApiPrincipal> principal = apiKeyAuthenticator.authenticate(apiKey);
            if (principal.isPresent()) {
                return principal;
            }
        }

        if (properties.isJwtEnabled() && authorization != null) {
            String normalized = authorization.trim();
            String token = normalized.regionMatches(true, 0, "Bearer ", 0, 7)
                    ? normalized.substring(7)
                    : normalized;
            return jwtAuthenticator.authenticate(token);
        }
        return Optional.empty();
    }

    private boolean isAuthorized(ApiPrincipal principal, long uid) {
        if (hasRole(principal, "ADMIN") || hasScope(principal, "admin")) {
            return true;
        }
        return ownsUid(principal.subject(), uid)
                && (hasScope(principal, "stream:read")
                || hasScope(principal, "user:stream")
                || hasScope(principal, "user:read"));
    }

    private static boolean ownsUid(String subject, long uid) {
        if (subject == null || subject.isBlank()) {
            return false;
        }
        String normalized = subject.trim();
        return normalized.equals(Long.toString(uid))
                || normalized.equalsIgnoreCase("user-" + uid)
                || normalized.equalsIgnoreCase("uid:" + uid);
    }

    private static boolean hasRole(ApiPrincipal principal, String role) {
        String required = normalizeRole(role);
        return principal.roles().stream()
                .map(UserStreamSubscriptionAuthorizer::normalizeRole)
                .anyMatch(required::equals);
    }

    private static boolean hasScope(ApiPrincipal principal, String scope) {
        String required = normalizeScope(scope);
        return principal.scopes().stream()
                .map(UserStreamSubscriptionAuthorizer::normalizeScope)
                .anyMatch(required::equals);
    }

    private static String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }

    private static String normalizeScope(String scope) {
        return scope == null ? "" : scope.trim().toLowerCase(Locale.ROOT);
    }

    public record UserStreamAuthorizationDecision(
            boolean allowed,
            HttpStatus status,
            String reason
    ) {
        private static UserStreamAuthorizationDecision allow() {
            return new UserStreamAuthorizationDecision(true, HttpStatus.OK, "ALLOWED");
        }

        private static UserStreamAuthorizationDecision rejected(HttpStatus status, String reason) {
            return new UserStreamAuthorizationDecision(false, status, reason);
        }
    }
}
