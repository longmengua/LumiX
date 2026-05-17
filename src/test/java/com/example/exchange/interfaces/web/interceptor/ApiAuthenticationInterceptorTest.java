/*
 * 檔案用途：測試 API authentication interceptor 的 401、403 與角色授權。
 */
package com.example.exchange.interfaces.web.interceptor;

import com.example.exchange.infra.config.ApiAuthProperties;
import com.example.exchange.interfaces.web.security.ApiKeyAuthenticator;
import com.example.exchange.interfaces.web.security.ApiPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ApiAuthenticationInterceptorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
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
