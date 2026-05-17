/*
 * 檔案用途：Web 攔截器，對交易、資金與管理 API 套用 rate limit、IP 白名單與安全審計。
 */
package com.example.exchange.interfaces.web.interceptor;

import com.example.exchange.infra.config.SecurityControlsProperties;
import com.example.exchange.interfaces.web.security.IpAllowlist;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
public class ProtectedApiSecurityInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "protectedApiStartTime";
    private static final String AUDIT_CATEGORY = "protectedApiAuditCategory";
    private static final String CLIENT_IP = "protectedApiClientIp";

    private final SecurityControlsProperties properties;
    private final Map<String, WindowCounter> rateLimitWindows = new ConcurrentHashMap<>();
    private final AtomicLong requestSequence = new AtomicLong();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!properties.isEnabled()) {
            return true;
        }

        String path = request.getRequestURI();
        String clientIp = resolveClientIp(request);
        String category = classify(path);

        request.setAttribute(START_TIME, System.currentTimeMillis());
        request.setAttribute(AUDIT_CATEGORY, category);
        request.setAttribute(CLIENT_IP, clientIp);

        if (properties.isIpAllowlistEnabled()
                && !IpAllowlist.allows(clientIp, properties.getIpAllowlist())) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "IP is not allowed");
            audit(request, response, category, clientIp, "REJECTED", "IP_NOT_ALLOWED", 0);
            return false;
        }

        if (properties.isRateLimitEnabled()
                && !consumeRateLimit(clientIp, category)) {
            response.setHeader("Retry-After", "60");
            writeError(response, 429, "Rate limit exceeded");
            audit(request, response, category, clientIp, "REJECTED", "RATE_LIMIT_EXCEEDED", 0);
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!properties.isEnabled()) {
            return;
        }

        String category = (String) request.getAttribute(AUDIT_CATEGORY);
        String clientIp = (String) request.getAttribute(CLIENT_IP);
        Long startTime = (Long) request.getAttribute(START_TIME);
        long durationMs = startTime == null ? 0 : System.currentTimeMillis() - startTime;
        String result = response.getStatus() >= 400 ? "FAILED" : "ALLOWED";
        String reason = ex == null ? "COMPLETED" : ex.getClass().getSimpleName();

        audit(request, response, category, clientIp, result, reason, durationMs);
    }

    private boolean consumeRateLimit(String clientIp, String category) {
        long minuteBucket = Instant.now().getEpochSecond() / 60;
        String key = clientIp + ":" + category;

        cleanupIfNeeded();

        WindowCounter counter = rateLimitWindows.computeIfAbsent(
                key,
                ignored -> new WindowCounter(minuteBucket)
        );

        synchronized (counter) {
            if (counter.minuteBucket != minuteBucket) {
                counter.minuteBucket = minuteBucket;
                counter.count = 0;
            }

            counter.count++;
            counter.lastSeenSequence = requestSequence.incrementAndGet();

            return counter.count <= properties.getRequestsPerMinute();
        }
    }

    private void cleanupIfNeeded() {
        int maxTrackedKeys = properties.getMaxTrackedKeys();
        if (rateLimitWindows.size() <= maxTrackedKeys) {
            return;
        }

        long threshold = requestSequence.get() - maxTrackedKeys;
        rateLimitWindows.entrySet().removeIf(entry -> entry.getValue().lastSeenSequence < threshold);
    }

    private void audit(HttpServletRequest request, HttpServletResponse response, String category,
                       String clientIp, String result, String reason, long durationMs) {
        if (!properties.isAuditEnabled()) {
            return;
        }

        log.info(
                "SECURITY_AUDIT category={} result={} reason={} method={} path={} clientIp={} status={} durationMs={} requestId={}",
                category,
                result,
                reason,
                request.getMethod(),
                request.getRequestURI(),
                clientIp,
                response.getStatus(),
                durationMs,
                response.getHeader("X-Request-Id")
        );
    }

    private String resolveClientIp(HttpServletRequest request) {
        String configuredHeader = properties.getClientIpHeader();
        if (configuredHeader != null && !configuredHeader.isBlank()) {
            String forwarded = request.getHeader(configuredHeader);
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private String classify(String path) {
        if (path == null) {
            return "UNKNOWN";
        }

        if (path.startsWith("/api/order")
                || path.startsWith("/api/prediction/orders")
                || path.startsWith("/api/prediction/session")) {
            return "TRADING";
        }

        if (path.startsWith("/api/margin")
                || path.startsWith("/api/prediction/approve")) {
            return "FUNDS";
        }

        if (path.startsWith("/api/risk")
                || path.startsWith("/api/recovery")
                || path.startsWith("/api/prediction/clob")
                || path.startsWith("/api/prediction/markets")
                || path.startsWith("/api/prediction/ws")) {
            return "ADMIN";
        }

        return "PROTECTED";
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

    private static class WindowCounter {
        private long minuteBucket;
        private int count;
        private long lastSeenSequence;

        private WindowCounter(long minuteBucket) {
            this.minuteBucket = minuteBucket;
        }
    }
}
