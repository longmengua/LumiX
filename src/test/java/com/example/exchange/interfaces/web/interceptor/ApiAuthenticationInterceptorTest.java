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
    /**
     * 流程：關閉 auth 設定 -> 呼叫 protected path -> 驗證 interceptor 不檢查 header 並放行。
     */
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
    /**
     * 流程：開啟 API key auth -> request 不帶 X-API-Key -> 驗證 preHandle 回 false 與 401 body。
     */
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
    /**
     * 流程：設定 admin role/scope -> 呼叫管理端 risk API -> 驗證放行並把 principal 寫進 request。
     */
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
    /**
     * 流程：設定 trader role/scope -> 呼叫 admin-only API -> 驗證授權失敗且 response 為 403。
     */
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
    @DisplayName("做市商後台 API 也歸類為管理員 API")
    /**
     * 流程：trader key 呼叫 /api/market-maker/profiles -> classifier 應視為 admin-only 並拒絕。
     */
    void rejectsMarketMakerAdminApiForTraderApiKey() throws Exception {
        ApiAuthenticationInterceptor interceptor =
                new ApiAuthenticationInterceptor(enabledProperties("trader-key", "ROLE_TRADER", "trade:write"), objectMapper);
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        boolean allowed =
                interceptor.preHandle(request("POST", "/api/market-maker/profiles", "trader-key"), response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Permission denied");
    }

    @Test
    @DisplayName("通用 admin API 也歸類為管理員 API")
    /**
     * 流程：trader key 呼叫 /api/admin/market-config -> classifier 應視為 admin-only 並拒絕。
     */
    void rejectsGenericAdminApiForTraderApiKey() throws Exception {
        ApiAuthenticationInterceptor interceptor =
                new ApiAuthenticationInterceptor(enabledProperties("trader-key", "ROLE_TRADER", "trade:write"), objectMapper);
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        boolean allowed =
                interceptor.preHandle(request("GET", "/api/admin/market-config", "trader-key"), response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Permission denied");
    }

    @Test
    @DisplayName("交易員 API key 可呼叫下單 API 並寫入 principal")
    /**
     * 流程：設定 trader trade:write scope -> 呼叫下單 API -> 驗證放行並留下 principal subject。
     */
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

    /**
     * 建立啟用狀態的 auth 設定，把 raw key 轉成 sha256 設定格式並注入 roles/scopes。
     */
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

    /**
     * 建立 mock request；apiKey 為 null 時刻意不加 header，用來測 401 分支。
     */
    private MockHttpServletRequest request(String method, String path, String apiKey) {
        MockHttpServletRequest request =
                new MockHttpServletRequest(method, path);
        if (apiKey != null) {
            request.addHeader("X-API-Key", apiKey);
        }
        return request;
    }
}
