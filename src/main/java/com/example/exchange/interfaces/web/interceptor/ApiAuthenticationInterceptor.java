/*
 * 檔案用途：Web 攔截器，對受保護 API 套用 API key / JWT 驗證與角色、scope 授權。
 */
package com.example.exchange.interfaces.web.interceptor;

import com.example.exchange.infra.config.ApiAuthProperties;
import com.example.exchange.interfaces.web.security.ApiKeyAuthenticator;
import com.example.exchange.interfaces.web.security.ApiPrincipal;
import com.example.exchange.interfaces.web.security.JwtAuthenticator;
import com.example.exchange.interfaces.web.security.ProtectedApiCategory;
import com.example.exchange.interfaces.web.security.ProtectedApiClassifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class ApiAuthenticationInterceptor implements HandlerInterceptor {

    public static final String PRINCIPAL_ATTRIBUTE = "apiPrincipal";

    private final ApiAuthProperties properties;
    private final ApiKeyAuthenticator apiKeyAuthenticator;
    private final JwtAuthenticator jwtAuthenticator;

    public ApiAuthenticationInterceptor(ApiAuthProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.apiKeyAuthenticator = new ApiKeyAuthenticator(properties.getApiKeys());
        this.jwtAuthenticator = new JwtAuthenticator(
                objectMapper,
                properties.getJwtHmacSecret(),
                properties.getClockSkewSeconds()
        );
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!properties.isEnabled()) {
            return true;
        }

        Optional<ApiPrincipal> principal =
                authenticate(request);

        if (principal.isEmpty()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication is required");
            audit(request, response, null, "REJECTED", "AUTH_REQUIRED");
            return false;
        }

        ProtectedApiCategory category =
                ProtectedApiClassifier.classify(request.getRequestURI());

        if (!isAuthorized(principal.get(), category)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Permission denied");
            audit(request, response, principal.get(), "REJECTED", "PERMISSION_DENIED");
            return false;
        }

        request.setAttribute(PRINCIPAL_ATTRIBUTE, principal.get());
        audit(request, response, principal.get(), "ALLOWED", category.name());
        return true;
    }

    private Optional<ApiPrincipal> authenticate(HttpServletRequest request) {
        if (properties.isApiKeyEnabled()) {
            Optional<ApiPrincipal> apiKeyPrincipal =
                    apiKeyAuthenticator.authenticate(request.getHeader(properties.getApiKeyHeader()));
            if (apiKeyPrincipal.isPresent()) {
                return apiKeyPrincipal;
            }
        }

        if (properties.isJwtEnabled()) {
            String authorization =
                    request.getHeader("Authorization");
            if (authorization != null
                    && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return jwtAuthenticator.authenticate(authorization.substring(7));
            }
        }

        return Optional.empty();
    }

    private boolean isAuthorized(ApiPrincipal principal, ProtectedApiCategory category) {
        return switch (category) {
            case ADMIN -> hasRole(principal, "ADMIN") || hasScope(principal, "admin");
            case FUNDS -> hasRole(principal, "ADMIN")
                    || hasRole(principal, "FUNDS")
                    || hasScope(principal, "funds")
                    || hasScope(principal, "funds:write");
            case TRADING -> hasRole(principal, "ADMIN")
                    || hasRole(principal, "TRADER")
                    || hasRole(principal, "USER")
                    || hasScope(principal, "trade")
                    || hasScope(principal, "trade:write")
                    || hasScope(principal, "session:write");
            case PROTECTED, UNKNOWN -> !principal.roles().isEmpty() || !principal.scopes().isEmpty();
        };
    }

    private boolean hasRole(ApiPrincipal principal, String role) {
        String normalizedRequired =
                normalizeRole(role);
        return principal.roles().stream()
                .map(this::normalizeRole)
                .anyMatch(normalizedRequired::equals);
    }

    private boolean hasScope(ApiPrincipal principal, String scope) {
        String normalizedRequired =
                normalizeScope(scope);
        Set<String> scopes = principal.scopes();
        return scopes.stream()
                .map(this::normalizeScope)
                .anyMatch(normalizedRequired::equals);
    }

    private String normalizeRole(String role) {
        String normalized =
                role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_")
                ? normalized.substring("ROLE_".length())
                : normalized;
    }

    private String normalizeScope(String scope) {
        return scope == null ? "" : scope.trim().toLowerCase(Locale.ROOT);
    }

    private void audit(HttpServletRequest request, HttpServletResponse response, ApiPrincipal principal,
                       String result, String reason) {
        log.info(
                "AUTH_AUDIT result={} reason={} method={} path={} status={} subject={} credentialType={} requestId={}",
                result,
                reason,
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                principal == null ? null : principal.subject(),
                principal == null ? null : principal.credentialType(),
                response.getHeader("X-Request-Id")
        );
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"code\":" + status + ",\"message\":\"" + message + "\"}"
        );
    }
}
