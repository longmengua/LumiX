/*
 * 檔案用途：Web 攔截器，處理 HTTP 請求層的橫切邏輯。
 */
package com.example.exchange.interfaces.web.interceptor;

import com.example.exchange.domain.util.SensitiveLogSanitizer;
import com.example.exchange.infra.tracing.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * RequestLoggingInterceptor
 * -------------------------
 * - 功能：
 *   1. 每個請求分配唯一 UUID，方便日誌追蹤
 *   2. 記錄請求 Method、URI
 *   3. 計算請求耗時
 *   4. 統一輸出例外
 */
@Slf4j
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "requestStartTime";
    private static final String REQUEST_ID = "requestUUID";
    private static final String CORRELATION_ID = "correlationId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = firstPresent(
                TraceContext.normalize(request.getHeader(TraceContext.REQUEST_ID_HEADER)),
                TraceContext.newRequestId()
        );
        String correlationId = firstPresent(
                TraceContext.normalize(request.getHeader(TraceContext.CORRELATION_ID_HEADER)),
                requestId
        );
        request.setAttribute(REQUEST_ID, requestId);
        request.setAttribute(CORRELATION_ID, correlationId);
        TraceContext.put(requestId, correlationId);

        // 紀錄開始時間
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME, startTime);

        // 加到 Response Header，方便前端或其他服務追蹤
        response.setHeader(TraceContext.REQUEST_ID_HEADER, requestId);
        response.setHeader(TraceContext.CORRELATION_ID_HEADER, correlationId);

        log.info("[{}] 請求開始 => correlationId={}, Method: {}, URI: {}",
                requestId, correlationId, request.getMethod(), safeRequestUri(request));

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        String requestId = (String) request.getAttribute(REQUEST_ID);
        log.debug("[{}] Controller 執行完成", requestId);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String requestId = (String) request.getAttribute(REQUEST_ID);
        String correlationId = (String) request.getAttribute(CORRELATION_ID);
        Long startTime = (Long) request.getAttribute(START_TIME);

        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] 請求完成 => correlationId={}, 耗時 {} ms", requestId, correlationId, duration);
        }

        if (ex != null) {
            log.error(
                    "[{}] 請求發生例外: type={}, message={}",
                    requestId,
                    ex.getClass().getName(),
                    SensitiveLogSanitizer.sanitize(ex.getMessage())
            );
        }
        TraceContext.clear();
    }

    private String safeRequestUri(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String uri = queryString == null || queryString.isBlank()
                ? request.getRequestURI()
                : request.getRequestURI() + "?" + queryString;

        return SensitiveLogSanitizer.sanitize(uri);
    }

    private static String firstPresent(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
