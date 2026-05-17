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

    /** 對外傳遞單一 request identity 的 header name。 */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    /** 對外傳遞跨服務 correlation identity 的 header name。 */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** MDC trace id，預設與 correlation id 對齊。 */
    public static final String TRACE_ID = "traceId";
    public static final String REQUEST_ID = "requestId";
    public static final String CORRELATION_ID = "correlationId";

    private TraceContext() {
    }

    public static String newRequestId() {
        return UUID.randomUUID().toString();
    }

    /** 將 request/correlation id 正規化後放入 MDC，供 logging pattern 與下游 header 使用。 */
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

    /** 取出目前 MDC 中可安全傳遞到 outbox、Kafka 或外部 API 的 tracing headers。 */
    public static Map<String, String> currentHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        putIfPresent(headers, REQUEST_ID_HEADER, MDC.get(REQUEST_ID));
        putIfPresent(headers, CORRELATION_ID_HEADER, MDC.get(CORRELATION_ID));
        return headers;
    }

    /** 清理本 request 的 MDC 欄位，避免 servlet thread reuse 時污染下一個請求。 */
    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(REQUEST_ID);
        MDC.remove(CORRELATION_ID);
    }

    /** 移除空白、換行與敏感資訊，避免 tracing id 被用來污染 log。 */
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
