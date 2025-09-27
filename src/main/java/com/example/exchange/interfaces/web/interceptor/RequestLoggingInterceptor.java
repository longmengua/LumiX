package com.example.exchange.interfaces.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 請求日誌攔截器（簡化版）
 * - 記錄方法、路徑、查詢參數與處理時間
 * - 生產中可加入 traceId、uid、body 限流輸出等
 */
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    private static final String ATTR_START = "req_start_ns";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        request.setAttribute(ATTR_START, System.nanoTime());
        log.info("[REQ] {} {}{}", request.getMethod(), request.getRequestURI(),
                request.getQueryString() != null ? ("?" + request.getQueryString()) : "");
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        Object st = request.getAttribute(ATTR_START);
        long costMs = (st instanceof Long) ? (System.nanoTime() - (Long) st) / 1_000_000L : -1L;
        log.info("[RES] {} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), costMs);
        if (ex != null) {
            log.warn("[ERR] {} {} ex: {}", request.getMethod(), request.getRequestURI(), ex.toString());
        }
    }
}
