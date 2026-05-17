/*
 * 檔案用途：基礎設施工具，集中處理 request id 與 correlation id 的 MDC 與傳遞 header。
 */
package com.example.exchange.infra.tracing;

import com.example.exchange.domain.util.SensitiveLogSanitizer;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class TraceContext {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    public static final String TRACE_ID = "traceId";
    public static final String REQUEST_ID = "requestId";
    public static final String CORRELATION_ID = "correlationId";

    private TraceContext() {
    }

    public static String newRequestId() {
        return UUID.randomUUID().toString();
    }

    public static void put(String requestId, String correlationId) {
        String safeRequestId = normalize(requestId);
        String safeCorrelationId = normalize(correlationId);
        if (safeRequestId != null) {
            MDC.put(REQUEST_ID, safeRequestId);
        }
        if (safeCorrelationId != null) {
            MDC.put(CORRELATION_ID, safeCorrelationId);
            MDC.put(TRACE_ID, safeCorrelationId);
        }
    }

    public static Map<String, String> currentHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        putIfPresent(headers, REQUEST_ID_HEADER, MDC.get(REQUEST_ID));
        putIfPresent(headers, CORRELATION_ID_HEADER, MDC.get(CORRELATION_ID));
        return headers;
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(REQUEST_ID);
        MDC.remove(CORRELATION_ID);
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String sanitized = SensitiveLogSanitizer.sanitize(value.trim())
                .replace('\r', '_')
                .replace('\n', '_');
        return sanitized.isBlank() ? null : sanitized;
    }

    private static void putIfPresent(Map<String, String> headers, String name, String value) {
        String normalized = normalize(value);
        if (normalized != null) {
            headers.put(name, normalized);
        }
    }
}
