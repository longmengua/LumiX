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

/** 最高管理員明確切換合約商品；具體啟用條件在 service 內驗證。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/trading/futures/instruments/status")
public class FuturesInstrumentStatusController {
    private final FuturesInstrumentStatusService service;
    private final ObjectMapper json;
    private final String allowedOrigin;

    public FuturesInstrumentStatusController(FuturesInstrumentStatusService service, ObjectMapper json,
            @Value("${lumix.trading.admin-allowed-origin:}") String allowedOrigin) {
        this.service=service;this.allowedOrigin=allowedOrigin;
        this.json=json.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    public record Command(String marketSymbol, String status) { }

    @PostMapping(consumes="application/json")
    public ResponseEntity<FuturesInstrumentStatusService.Status> change(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            HttpServletRequest request) throws IOException {
        if (allowedOrigin.isBlank() || !allowedOrigin.equals(request.getHeader("Origin"))
                || !"trading-policy".equals(request.getHeader("X-LumiX-Intent"))
                || "cross-site".equals(request.getHeader("Sec-Fetch-Site")))
            throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR);
        byte[] body=request.getInputStream().readNBytes(4097);
        if (body.length>4096) throw new IllegalArgumentException("status command exceeds size limit");
        Command command=json.readValue(body,Command.class);
        return ResponseEntity.ok().header("Cache-Control","no-store")
                .body(service.change(actor,command.marketSymbol(),command.status()));
    }
}
