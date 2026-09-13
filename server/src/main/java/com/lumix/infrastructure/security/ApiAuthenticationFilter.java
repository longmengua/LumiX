package com.lumix.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiErrorResponse;
import com.lumix.api.error.ApiException;
import com.lumix.common.RequestId;
import com.lumix.user.auth.application.UserAuthenticationService;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.SessionSecret;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * `/api` 的全域 authentication gate。
 *
 * <p>只有明列的帳號建立／登入／密碼重設入口可以匿名；其餘 API 在進 controller 前必須驗證 HttpOnly
 * session，並把去敏後的 principal 放進 request attribute。這避免未來新增 endpoint 時遺漏個別鑒權。</p>
 */
@Component
@Profile("infrastructure")
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class ApiAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_USER_ATTRIBUTE = "lumix.authenticatedUser";
    private static final Set<String> ANONYMOUS_POST_PATHS = Set.of(
        "/api/v1/auth/captcha/challenge/verify",
        "/api/v1/auth/register",
        "/api/v1/auth/register/verify-email",
        "/api/v1/auth/login",
        "/api/v1/auth/login-verification/decision",
        "/api/v1/auth/login-verification/complete",
        "/api/v1/auth/password/forgot",
        "/api/v1/auth/password/reset",
        "/api/admin/v1/auth/password/forgot",
        "/api/admin/v1/auth/password/reset"
    );

    private final UserAuthenticationService authenticationService;
    private final UserAuthenticationProperties authenticationProperties;
    private final ObjectMapper objectMapper;

    public ApiAuthenticationFilter(
        UserAuthenticationService authenticationService,
        UserAuthenticationProperties authenticationProperties,
        ObjectMapper objectMapper
    ) {
        this.authenticationService = authenticationService;
        this.authenticationProperties = authenticationProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!request.getRequestURI().startsWith("/api/")) {
            return true;
        }
        if ("GET".equals(request.getMethod()) && "/api/v1/auth/captcha/challenge".equals(request.getRequestURI())) {
            return true;
        }
        return "POST".equals(request.getMethod()) && ANONYMOUS_POST_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            SessionSecret session = readSessionCookie(request);
            AuthenticatedUser user = authenticationService.authenticate(session);
            request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, user);
            filterChain.doFilter(request, response);
        } catch (ApiException exception) {
            writeError(response, exception.getErrorCode());
        }
    }

    private SessionSecret readSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (authenticationProperties.getCookieName().equals(cookie.getName())) {
                    return UserAuthenticationService.parseCookieValue(cookie.getValue());
                }
            }
        }
        throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
    }

    private void writeError(HttpServletResponse response, ApiErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(
            errorCode, new RequestId(UUID.randomUUID().toString()), null
        ));
    }
}
