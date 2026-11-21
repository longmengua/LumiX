package com.lumix.trading.runtime;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 最高管理員可提交完整合約風險政策；提交結果始終是 HALTED 商品。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/trading/futures/policies")
public class FuturesRiskPolicyAdministrationController {
    private final FuturesRiskPolicyAdministrationService service;
    private final ObjectMapper json;
    private final String allowedOrigin;

    public FuturesRiskPolicyAdministrationController(FuturesRiskPolicyAdministrationService service, ObjectMapper json,
            @Value("${lumix.trading.admin-allowed-origin:}") String allowedOrigin) {
        this.service = service;
        this.allowedOrigin = allowedOrigin;
        this.json = json.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    /** 與現貨政策命令使用相同的來源及意圖 gate；拒絕過大或含未知欄位的政策。 */
    @PostMapping(consumes = "application/json")
    public ResponseEntity<FuturesRiskPolicy> publish(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            HttpServletRequest request) throws IOException {
        if (allowedOrigin.isBlank() || !allowedOrigin.equals(request.getHeader("Origin"))
                || !"trading-policy".equals(request.getHeader("X-LumiX-Intent"))
                || "cross-site".equals(request.getHeader("Sec-Fetch-Site")))
            throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR);
        byte[] body = request.getInputStream().readNBytes(16385);
        if (body.length > 16384) throw new IllegalArgumentException("policy size exceeds limit");
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(service.publish(actor, json.readValue(body, FuturesRiskPolicy.class)));
    }
}
