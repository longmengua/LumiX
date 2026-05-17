/*
 * 檔案用途：測試受保護 API 攔截器的 IP 白名單與 rate limit 行為。
 */
package com.example.exchange.interfaces.web.interceptor;

import com.example.exchange.infra.config.SecurityControlsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆蓋受保護 API 的安全前置檢查：IP allowlist、允許分支與 per-IP rate limit。
 */
class ProtectedApiSecurityInterceptorTest {

    @Test
    @DisplayName("來源 IP 不在 allowlist 時拒絕請求")
    /**
     * 流程：只允許 10.0.0.0/8 -> 用外部 IP 打交易 API -> 驗證拒絕與錯誤訊息。
     */
    void rejectsIpOutsideAllowlist() throws Exception {
        SecurityControlsProperties properties = new SecurityControlsProperties();
        properties.setIpAllowlistEnabled(true);
        properties.setIpAllowlist(List.of("10.0.0.0/8"));

        ProtectedApiSecurityInterceptor interceptor =
                new ProtectedApiSecurityInterceptor(properties);
        MockHttpServletRequest request =
                request("POST", "/api/order/place", "203.0.113.1");
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        boolean allowed =
                interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("IP is not allowed");
    }

    @Test
    @DisplayName("來源 IP 落在 allowlist CIDR 內時放行")
    /**
     * 流程：只允許 10.0.0.0/8 -> 用 10.1.2.3 打交易 API -> 驗證安全前置檢查放行。
     */
    void allowsIpInsideAllowlist() throws Exception {
        SecurityControlsProperties properties = new SecurityControlsProperties();
        properties.setIpAllowlistEnabled(true);
        properties.setIpAllowlist(List.of("10.0.0.0/8"));

        ProtectedApiSecurityInterceptor interceptor =
                new ProtectedApiSecurityInterceptor(properties);
        MockHttpServletRequest request =
                request("POST", "/api/order/place", "10.1.2.3");
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        boolean allowed =
                interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("同一 IP 超過每分鐘限制時回 429 與 Retry-After")
    /**
     * 流程：設定每分鐘只允許 1 次 -> 同 IP 連打兩次 -> 驗證第二次被 rate limit 擋下。
     */
    void rejectsWhenRateLimitExceeded() throws Exception {
        SecurityControlsProperties properties = new SecurityControlsProperties();
        properties.setRequestsPerMinute(1);
        properties.setIpAllowlistEnabled(false);

        ProtectedApiSecurityInterceptor interceptor =
                new ProtectedApiSecurityInterceptor(properties);

        // 第一個請求消耗掉唯一可用額度，第二個請求應觸發 rate limit。
        assertThat(interceptor.preHandle(
                request("POST", "/api/order/place", "10.1.2.3"),
                new MockHttpServletResponse(),
                new Object()
        )).isTrue();

        MockHttpServletResponse secondResponse =
                new MockHttpServletResponse();

        boolean secondAllowed =
                interceptor.preHandle(
                        request("POST", "/api/order/place", "10.1.2.3"),
                        secondResponse,
                        new Object()
                );

        assertThat(secondAllowed).isFalse();
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getHeader("Retry-After")).isEqualTo("60");
    }

    /**
     * 建立帶 remoteAddr 的 mock request，讓 IP allowlist 與 rate-limit 測試使用同一入口。
     */
    private MockHttpServletRequest request(String method, String path, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
