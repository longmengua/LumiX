/*
 * 檔案用途：Web 攔截器，處理 HTTP 請求層的橫切邏輯。
 */
package com.example.exchange.interfaces.web.interceptor;

import com.example.exchange.domain.util.SensitiveLogSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 產生唯一的 Request UUID
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID, requestId);

        // 紀錄開始時間
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME, startTime);

        // 加到 Response Header，方便前端或其他服務追蹤
        response.setHeader("X-Request-Id", requestId);

        log.info("[{}] 請求開始 => Method: {}, URI: {}",
                requestId, request.getMethod(), safeRequestUri(request));

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
        Long startTime = (Long) request.getAttribute(START_TIME);

        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] 請求完成 => 耗時 {} ms", requestId, duration);
        }

        if (ex != null) {
            log.error(
                    "[{}] 請求發生例外: type={}, message={}",
                    requestId,
                    ex.getClass().getName(),
                    SensitiveLogSanitizer.sanitize(ex.getMessage())
            );
        }
    }

    private String safeRequestUri(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String uri = queryString == null || queryString.isBlank()
                ? request.getRequestURI()
                : request.getRequestURI() + "?" + queryString;

        return SensitiveLogSanitizer.sanitize(uri);
    }
}
