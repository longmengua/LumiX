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
import org.springframework.web.bind.annotation.*;

/** 最高管理員明確提交完整政策；不提供零費用或 sandbox 參數的一鍵啟用。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/trading/spot/policies")
public class TradingPolicyAdministrationController {
    private final TradingPolicyAdministrationService service;
    private final ObjectMapper json;
    private final String origin;
    public TradingPolicyAdministrationController(TradingPolicyAdministrationService service,ObjectMapper json,
            @Value("${lumix.trading.admin-allowed-origin:}") String origin){
        this.service=service;this.origin=origin;
        this.json=json.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }
    /** 版本號本身是 durable 冪等識別；同版不同內容拒絕，與新單共享交易鎖避免政策切換競態。 */
    @PostMapping(consumes="application/json")
    public ResponseEntity<TradingExecutionPolicy> publish(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,HttpServletRequest request) throws IOException {
        if(origin.isBlank()||!origin.equals(request.getHeader("Origin"))||!"trading-policy".equals(request.getHeader("X-LumiX-Intent"))
                ||"cross-site".equals(request.getHeader("Sec-Fetch-Site")))throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR);
        byte[] body=request.getInputStream().readNBytes(16385);
        if(body.length>16384)throw new IllegalArgumentException("policy size exceeds limit");
        return ResponseEntity.ok().header("Cache-Control","no-store").body(service.publishSpot(user,json.readValue(body,TradingExecutionPolicy.class)));
    }
}
