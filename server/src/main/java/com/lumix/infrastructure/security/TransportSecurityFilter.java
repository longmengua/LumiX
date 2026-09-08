package com.lumix.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiErrorResponse;
import com.lumix.common.RequestId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * API transport security gate。
 *
 * <p>HTTPS 同時提供機密性、完整性與伺服器身分驗證；不得用自製前端 payload 加密取代 TLS。正式
 * deployment 開啟 requireHttps 後，未加密 API request 必須被拒絕，而不是 redirect 後繼續處理。</p>
 */
@Component
@Profile("infrastructure")
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TransportSecurityFilter extends OncePerRequestFilter {

    private final ApiSecurityProperties properties;
    private final ObjectMapper objectMapper;

    public TransportSecurityFilter(ApiSecurityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        applyResponseSecurityHeaders(response);
        if (properties.isRequireHttps() && !request.isSecure()) {
            writeError(response, ApiErrorCode.TRANSPORT_SECURITY_REQUIRED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static void applyResponseSecurityHeaders(HttpServletResponse response) {
        // 認證 response 不得由 browser/proxy 快取，避免使用者投影留在共享快取中。
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
    }

    private void writeError(HttpServletResponse response, ApiErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(
            errorCode, new RequestId(UUID.randomUUID().toString()), null
        ));
    }
}
