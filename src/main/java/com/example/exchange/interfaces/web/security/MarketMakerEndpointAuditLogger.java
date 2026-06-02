/*
 * 檔案用途：輸出 market-maker effectful endpoint audit 欄位，避免 log 中暴露 approval token 原文。
 */
package com.example.exchange.interfaces.web.security;

import com.example.exchange.infra.tracing.TraceContext;
import com.example.exchange.interfaces.web.interceptor.ApiAuthenticationInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class MarketMakerEndpointAuditLogger {

    public static final String OPERATOR_ID_HEADER = "X-Operator-Id";
    public static final String APPROVAL_NOT_APPLICABLE = "NOT_APPLICABLE";

    public void audit(
            HttpServletRequest request,
            String endpoint,
            String resource,
            String approvalTokenOutcome,
            String result,
            String reason
    ) {
        MarketMakerEndpointAuditRecord record =
                record(request, endpoint, resource, approvalTokenOutcome, result, reason);
        log.info(
                "MARKET_MAKER_ENDPOINT_AUDIT endpoint={} resource={} result={} reason={} "
                        + "operatorId={} operatorSubject={} credentialType={} approvalTokenOutcome={} requestId={}",
                record.endpoint(),
                record.resource(),
                record.result(),
                record.reason(),
                record.operatorId(),
                record.operatorSubject(),
                record.credentialType(),
                record.approvalTokenOutcome(),
                record.requestId()
        );
    }

    MarketMakerEndpointAuditRecord record(
            HttpServletRequest request,
            String endpoint,
            String resource,
            String approvalTokenOutcome,
            String result,
            String reason
    ) {
        ApiPrincipal principal = principal(request);
        Map<String, String> traceHeaders = TraceContext.currentHeaders();
        return new MarketMakerEndpointAuditRecord(
                normalize(endpoint),
                normalize(resource),
                normalize(result),
                normalize(reason),
                normalize(request.getHeader(OPERATOR_ID_HEADER)),
                principal == null ? null : normalize(principal.subject()),
                principal == null ? null : normalize(principal.credentialType()),
                normalize(approvalTokenOutcome),
                traceHeaders.get(TraceContext.REQUEST_ID_HEADER)
        );
    }

    public static String approvalOutcome(String approvalToken, boolean success, Throwable failure) {
        boolean provided = approvalToken != null && !approvalToken.isBlank();
        if (success) {
            return provided ? "PROVIDED_ACCEPTED" : "NOT_PROVIDED_ACCEPTED";
        }
        if (isApprovalFailure(failure)) {
            return provided ? "PROVIDED_REJECTED" : "NOT_PROVIDED_REJECTED";
        }
        return provided ? "PROVIDED_NOT_EVALUATED" : "NOT_PROVIDED_NOT_EVALUATED";
    }

    private static boolean isApprovalFailure(Throwable failure) {
        return failure != null
                && failure.getMessage() != null
                && failure.getMessage().toLowerCase().contains("approval");
    }

    private static ApiPrincipal principal(HttpServletRequest request) {
        Object principal = request.getAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE);
        return principal instanceof ApiPrincipal apiPrincipal ? apiPrincipal : null;
    }

    private static String normalize(String value) {
        return TraceContext.normalize(value);
    }

    record MarketMakerEndpointAuditRecord(
            String endpoint,
            String resource,
            String result,
            String reason,
            String operatorId,
            String operatorSubject,
            String credentialType,
            String approvalTokenOutcome,
            String requestId
    ) {
    }
}
