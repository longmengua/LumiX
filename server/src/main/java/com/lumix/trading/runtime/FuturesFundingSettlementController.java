package com.lumix.trading.runtime;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 受最高管理員管控的 funding 週期命令；同商品同時點只能結算一次。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/trading/futures/funding-cycles")
public class FuturesFundingSettlementController {
    private final FuturesFundingSettlementService service;
    private final ObjectMapper json;
    private final String origin;

    public FuturesFundingSettlementController(FuturesFundingSettlementService service, ObjectMapper json,
            @Value("${lumix.trading.admin-allowed-origin:}") String origin) {
        this.service = service;
        this.origin = origin;
        this.json = json.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    public record Command(String marketSymbol, Instant periodEnd, String fundingRate) { }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<FuturesFundingSettlementService.Result> settle(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            HttpServletRequest request) throws IOException {
        if (origin.isBlank() || !origin.equals(request.getHeader("Origin"))
                || !"trading-policy".equals(request.getHeader("X-LumiX-Intent"))
                || "cross-site".equals(request.getHeader("Sec-Fetch-Site")))
            throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR);
        byte[] body = request.getInputStream().readNBytes(4097);
        if (body.length > 4096) throw new IllegalArgumentException("funding command exceeds size limit");
        Command command = json.readValue(body, Command.class);
        BigDecimal rate = TradingDecimal.signed(command.fundingRate());
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(service.settle(actor, command.marketSymbol(), command.periodEnd(), rate));
    }
}
