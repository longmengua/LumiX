package com.lumix.infrastructure.observability;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 為每個 API request 建立可安全搜尋的 request／trace correlation，並輸出單一 ECS completion event。
 * 不記錄 query、body、cookie、token、email、原始 IP 或 session ID，避免集中式日誌成為敏感資料副本。
 */
@Component
@Profile("infrastructure")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestCorrelationLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_ATTRIBUTE = "lumix.requestId";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestCorrelationLoggingFilter.class);
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");
    private static final Pattern TRACE_PARENT = Pattern.compile(
        "^([0-9a-f]{2})-([0-9a-f]{32})-([0-9a-f]{16})-([0-9a-f]{2})$"
    );
    private static final Pattern PATH_IDENTIFIER = Pattern.compile(
        "(?i)(?<=/)(?:[0-9a-f]{8}-[0-9a-f-]{27,}|[0-9]{7,}|[0-9a-f]{20,})(?=/|$)"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader("X-Request-Id"));
        String traceId = resolveTraceId(request.getHeader("traceparent"));
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader("X-Request-Id", requestId);
        HttpServletRequest correlated = new CorrelatedRequest(request, requestId);
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        long started = System.nanoTime();
        boolean failed = false;
        try {
            MDC.put("request.id", requestId);
            MDC.put("trace.id", traceId);
            chain.doFilter(correlated, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failed = true;
            throw exception;
        } finally {
            int status = failed && response.getStatus() < 500 ? 500 : response.getStatus();
            MDC.put("event.action", "http_request_completed");
            MDC.put("event.duration", Long.toString(System.nanoTime() - started));
            MDC.put("http.request.method", request.getMethod());
            MDC.put("http.response.status_code", Integer.toString(status));
            MDC.put("url.path", PATH_IDENTIFIER.matcher(request.getRequestURI()).replaceAll("{id}"));
            Object route = correlated.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if (route != null) MDC.put("http.route", route.toString());
            MDC.put("client.address_hash", digest(request.getRemoteAddr()));
            Object principal = correlated.getAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE);
            if (principal instanceof AuthenticatedUser user) {
                MDC.put("user.id_hash", digest(user.userId()));
                MDC.put("lumix.actor_type", request.getRequestURI().startsWith("/api/admin/") ? "ADMIN" : "USER");
            }
            if (status >= 500) LOGGER.error("HTTP request completed");
            else if (status >= 400) LOGGER.warn("HTTP request completed");
            else LOGGER.info("HTTP request completed");
            restore(previousContext);
        }
    }

    /** 讓既有 exception handler 讀到同一個安全 request ID，而不是各自生成無法串接的值。 */
    public static String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        return value instanceof String id && SAFE_REQUEST_ID.matcher(id).matches()
            ? id : resolveRequestId(request.getHeader("X-Request-Id"));
    }

    private static String resolveRequestId(String supplied) {
        return supplied != null && SAFE_REQUEST_ID.matcher(supplied).matches()
            ? supplied : UUID.randomUUID().toString();
    }

    private static String resolveTraceId(String traceParent) {
        if (traceParent != null) {
            var match = TRACE_PARENT.matcher(traceParent.toLowerCase(java.util.Locale.ROOT));
            if (match.matches() && !"ff".equals(match.group(1))
                && !"00000000000000000000000000000000".equals(match.group(2))
                && !"0000000000000000".equals(match.group(3))) return match.group(2);
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void restore(Map<String, String> previous) {
        MDC.clear();
        if (previous != null) MDC.setContextMap(previous);
    }

    private static final class CorrelatedRequest extends HttpServletRequestWrapper {
        private final String requestId;

        private CorrelatedRequest(HttpServletRequest request, String requestId) {
            super(request);
            this.requestId = requestId;
        }

        @Override
        public String getHeader(String name) {
            return "X-Request-Id".equalsIgnoreCase(name) ? requestId : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return "X-Request-Id".equalsIgnoreCase(name)
                ? Collections.enumeration(java.util.List.of(requestId)) : super.getHeaders(name);
        }
    }
}
