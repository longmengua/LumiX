/*
 * 檔案用途：測試 request/correlation id tracing 攔截器。
 */
package com.example.exchange.interfaces.web.interceptor;

import com.example.exchange.infra.tracing.TraceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆蓋 request logging/tracing 攔截器對 request id、correlation id、MDC 的建立與清理。
 */
class RequestLoggingInterceptorTest {

    @Test
    @DisplayName("沿用 incoming trace headers，回寫 response，完成後清掉 MDC")
    /**
     * 流程：request 帶 request/correlation headers -> preHandle 寫 response 與 MDC -> afterCompletion 清 MDC。
     */
    void usesIncomingTraceHeadersAndClearsMdcAfterCompletion() {
        RequestLoggingInterceptor interceptor = new RequestLoggingInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/order/place");
        request.addHeader(TraceContext.REQUEST_ID_HEADER, "req-1");
        request.addHeader(TraceContext.CORRELATION_ID_HEADER, "corr-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getHeader(TraceContext.REQUEST_ID_HEADER)).isEqualTo("req-1");
        assertThat(response.getHeader(TraceContext.CORRELATION_ID_HEADER)).isEqualTo("corr-1");
        assertThat(MDC.get(TraceContext.REQUEST_ID)).isEqualTo("req-1");
        assertThat(MDC.get(TraceContext.CORRELATION_ID)).isEqualTo("corr-1");
        assertThat(MDC.get(TraceContext.TRACE_ID)).isEqualTo("corr-1");

        // afterCompletion 是 MDC 清理點，避免同 thread 下一個請求繼承舊 trace。
        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.get(TraceContext.REQUEST_ID)).isNull();
        assertThat(MDC.get(TraceContext.CORRELATION_ID)).isNull();
        assertThat(MDC.get(TraceContext.TRACE_ID)).isNull();
    }

    @Test
    @DisplayName("缺少 headers 時自動產生 request id 並作為 correlation id")
    /**
     * 流程：request 不帶 trace headers -> preHandle 自動產生 request id -> 驗證 correlation id fallback 相同。
     */
    void createsCorrelationIdFromGeneratedRequestIdWhenMissing() {
        RequestLoggingInterceptor interceptor = new RequestLoggingInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/order/open");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());

        String requestId = response.getHeader(TraceContext.REQUEST_ID_HEADER);
        assertThat(requestId).isNotBlank();
        assertThat(response.getHeader(TraceContext.CORRELATION_ID_HEADER)).isEqualTo(requestId);

        interceptor.afterCompletion(request, response, new Object(), null);
    }
}
