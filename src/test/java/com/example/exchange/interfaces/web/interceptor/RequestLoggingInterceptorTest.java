/*
 * 檔案用途：測試 request/correlation id tracing 攔截器。
 */
package com.example.exchange.interfaces.web.interceptor;

import com.example.exchange.infra.tracing.TraceContext;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingInterceptorTest {

    @Test
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

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.get(TraceContext.REQUEST_ID)).isNull();
        assertThat(MDC.get(TraceContext.CORRELATION_ID)).isNull();
        assertThat(MDC.get(TraceContext.TRACE_ID)).isNull();
    }

    @Test
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
