/*
 * 檔案用途：測試 API authentication interceptor 的 401、403 與角色授權。
 */
package com.example.exchange.interfaces.web.interceptor;

import com.example.exchange.infra.config.ApiAuthProperties;
import com.example.exchange.interfaces.web.security.ApiKeyAuthenticator;
import com.example.exchange.interfaces.web.security.ApiPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆蓋 API 認證攔截器的核心決策：關閉 auth 時放行、缺 credentials 時拒絕、
 * 以及不同 role/scope 對管理 API 與交易 API 的授權結果。
 */
class ApiAuthenticationInterceptorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("auth 關閉時不檢查 credentials 並直接放行")
    void allowsWhenAuthDisabled() throws Exception {
        ApiAuthProperties properties =
                new ApiAuthProperties();
        properties.setEnabled(false);

        ApiAuthenticationInterceptor interceptor =
                new ApiAuthenticationInterceptor(properties, objectMapper);
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        boolean allowed =
                interceptor.preHandle(request("POST", "/api/risk/liquidate", null), response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("auth 開啟但未帶 credentials 時回 401")
    void rejectsMissingCredentialsWhenEnabled() throws Exception {
        ApiAuthenticationInterceptor interceptor =
                new ApiAuthenticationInterceptor(enabledProperties("admin-key", "ROLE_ADMIN", "admin"), objectMapper);
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        boolean allowed =
                interceptor.preHandle(request("POST", "/api/risk/liquidate", null), response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Authentication is required");
    }

    @Test
    @DisplayName("管理員 API key 可呼叫管理端 risk API")
    void allowsAdminApiForAdminApiKey() throws Exception {
        ApiAuthenticationInterceptor interceptor =
                new ApiAuthenticationInterceptor(enabledProperties("admin-key", "ROLE_ADMIN", "admin"), objectMapper);
        MockHttpServletRequest request =
                request("POST", "/api/risk/liquidate", "admin-key");
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        boolean allowed =
                interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat((ApiPrincipal) request.getAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE))
                .extracting(ApiPrincipal::subject)
                .isEqualTo("test-key");
    }

    @Test
    @DisplayName("交易員 API key 呼叫管理端 risk API 會回 403")
    void rejectsAdminApiForTraderApiKey() throws Exception {
        ApiAuthenticationInterceptor interceptor =
                new ApiAuthenticationInterceptor(enabledProperties("trader-key", "ROLE_TRADER", "trade:write"), objectMapper);
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        boolean allowed =
                interceptor.preHandle(request("POST", "/api/risk/liquidate", "trader-key"), response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Permission denied");
    }

    @Test
    @DisplayName("交易員 API key 可呼叫下單 API 並寫入 principal")
    void allowsTradingApiForTraderApiKey() throws Exception {
        ApiAuthenticationInterceptor interceptor =
                new ApiAuthenticationInterceptor(enabledProperties("trader-key", "ROLE_TRADER", "trade:write"), objectMapper);
        MockHttpServletRequest request =
                request("POST", "/api/order/place", "trader-key");
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        boolean allowed =
                interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat((ApiPrincipal) request.getAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE))
                .extracting(ApiPrincipal::subject)
                .isEqualTo("test-key");
    }

    private ApiAuthProperties enabledProperties(String apiKey, String roles, String scopes) {
        ApiAuthProperties properties =
                new ApiAuthProperties();
        properties.setEnabled(true);
        properties.setJwtEnabled(false);
        // 測試設定格式為 subject:sha256(apiKey):roles:scopes。
        properties.setApiKeys(
                "test-key:"
                        + ApiKeyAuthenticator.sha256Hex(apiKey)
                        + ":"
                        + roles
                        + ":"
                        + scopes
        );
        return properties;
    }

    private MockHttpServletRequest request(String method, String path, String apiKey) {
        MockHttpServletRequest request =
                new MockHttpServletRequest(method, path);
        if (apiKey != null) {
            request.addHeader("X-API-Key", apiKey);
        }
        return request;
    }
}
