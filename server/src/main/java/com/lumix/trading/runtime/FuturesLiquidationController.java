package com.lumix.trading.runtime;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.admin.superadmin.SuperAdminAccessService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理員只能觸發重新計算後確認低於維持保證金的強制減倉。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/trading/futures/liquidations")
public class FuturesLiquidationController {
    private final FuturesTradingExecutionService trading;
    private final SuperAdminAccessService access;
    private final ObjectMapper json;
    private final String allowedOrigin;

    public FuturesLiquidationController(FuturesTradingExecutionService trading, SuperAdminAccessService access,
            ObjectMapper json, @Value("${lumix.trading.admin-allowed-origin:}") String allowedOrigin) {
        this.trading = trading;
        this.access = access;
        this.allowedOrigin = allowedOrigin;
        this.json = json.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    public record Command(String ownerId, String marketSymbol, String positionSide) { }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<TradingOrderQueryService.OrderView> liquidate(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            @RequestHeader("Idempotency-Key") String key, HttpServletRequest request) throws IOException {
        if (allowedOrigin.isBlank() || !allowedOrigin.equals(request.getHeader("Origin"))
                || !"trading-policy".equals(request.getHeader("X-LumiX-Intent"))
                || "cross-site".equals(request.getHeader("Sec-Fetch-Site")))
            throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR);
        access.requireActiveSuperAdmin(actor);
        byte[] body = request.getInputStream().readNBytes(4097);
        if (body.length > 4096) throw new IllegalArgumentException("liquidation command exceeds size limit");
        Command command = json.readValue(body, Command.class);
        if (command.ownerId() == null || !command.ownerId().matches("[A-Za-z0-9_-]{1,64}"))
            throw new IllegalArgumentException("invalid position owner");
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(trading.liquidate(
                command.ownerId(), command.marketSymbol(), command.positionSide(), key));
    }
}
