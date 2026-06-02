/*
 * 檔案用途：測試 market-maker endpoint audit 欄位，不檢查易碎的 log formatting。
 */
package com.example.exchange.interfaces.web.security;

import com.example.exchange.infra.tracing.TraceContext;
import com.example.exchange.interfaces.web.interceptor.ApiAuthenticationInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MarketMakerEndpointAuditLoggerTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    @DisplayName("audit record 會帶 operator identity、credential type、operator id 與 request id")
    void recordIncludesOperatorIdentityAndRequestId() {
        TraceContext.put("req-1", "corr-1");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/market-maker/quotes");
        request.addHeader(MarketMakerEndpointAuditLogger.OPERATOR_ID_HEADER, "ops-user-1");
        request.setAttribute(
                ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE,
                new ApiPrincipal("admin-key", "API_KEY", Set.of("ADMIN"), Set.of("admin"))
        );
        MarketMakerEndpointAuditLogger logger = new MarketMakerEndpointAuditLogger();

        // 場景：後台 operator 發出 effectful 做市商 command，audit 欄位必須能追到 credential 與 request。
        MarketMakerEndpointAuditLogger.MarketMakerEndpointAuditRecord record =
                logger.record(
                        request,
                        "QUOTE_PLACEMENT",
                        "mm-1:BTCUSDT",
                        MarketMakerEndpointAuditLogger.APPROVAL_NOT_APPLICABLE,
                        "SUCCESS",
                        "PLACED"
                );

        assertThat(record.operatorId()).isEqualTo("ops-user-1");
        assertThat(record.operatorSubject()).isEqualTo("admin-key");
        assertThat(record.credentialType()).isEqualTo("API_KEY");
        assertThat(record.requestId()).isEqualTo("req-1");
        assertThat(record.approvalTokenOutcome()).isEqualTo("NOT_APPLICABLE");
    }

    @Test
    @DisplayName("approval outcome 只記錄結果分類，不暴露 token 原文")
    void approvalOutcomeClassifiesTokenResultWithoutRawToken() {
        assertThat(MarketMakerEndpointAuditLogger.approvalOutcome("secret-token", true, null))
                .isEqualTo("PROVIDED_ACCEPTED");
        assertThat(MarketMakerEndpointAuditLogger.approvalOutcome(null, true, null))
                .isEqualTo("NOT_PROVIDED_ACCEPTED");
        assertThat(MarketMakerEndpointAuditLogger.approvalOutcome("secret-token", false,
                new IllegalStateException("hedge execution operator approval required")))
                .isEqualTo("PROVIDED_REJECTED");
        assertThat(MarketMakerEndpointAuditLogger.approvalOutcome(null, false,
                new IllegalStateException("hedge execution operator approval required")))
                .isEqualTo("NOT_PROVIDED_REJECTED");
        assertThat(MarketMakerEndpointAuditLogger.approvalOutcome("secret-token", false,
                new IllegalArgumentException("bad ref prefix")))
                .isEqualTo("PROVIDED_NOT_EVALUATED");
    }
}
