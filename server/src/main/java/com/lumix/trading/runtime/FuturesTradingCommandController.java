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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 合約命令只接受認證 owner 與嚴格來源；風險政策或開關不足時拒絕新單。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/trading/futures/orders")
public class FuturesTradingCommandController {
    private final FuturesTradingExecutionService service;
    private final TradingCommandSecurity security;
    private final ObjectMapper json;
    private final boolean enabled;

    public FuturesTradingCommandController(FuturesTradingExecutionService service,
            TradingCommandSecurity security, ObjectMapper json,
            @Value("${lumix.trading.futures-enabled:false}") boolean enabled) {
        this.service = service;
        this.security = security;
        this.enabled = enabled;
        this.json = json.copy().disable(com.fasterxml.jackson.databind.MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.json.coercionConfigFor(com.fasterxml.jackson.databind.type.LogicalType.Textual)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Integer,
                        com.fasterxml.jackson.databind.cfg.CoercionAction.Fail)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Float,
                        com.fasterxml.jackson.databind.cfg.CoercionAction.Fail)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Boolean,
                        com.fasterxml.jackson.databind.cfg.CoercionAction.Fail);
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<TradingOrderQueryService.OrderView> place(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            @RequestHeader("Idempotency-Key") String key, HttpServletRequest request) throws IOException {
        security.require(request, false);
        if (!enabled) throw new ApiException(ApiErrorCode.SERVICE_UNAVAILABLE);
        byte[] body = request.getInputStream().readNBytes(4097);
        if (body.length > 4096) throw new IllegalArgumentException("futures order exceeds size limit");
        var command = json.readValue(body, FuturesTradingExecutionService.Command.class);
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(service.place(actor.userId(), command, key));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<TradingOrderQueryService.OrderView> cancel(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            @PathVariable String orderId, @RequestHeader("Idempotency-Key") String key,
            HttpServletRequest request) {
        security.require(request, false);
        if (orderId.length() > 64) throw new IllegalArgumentException("invalid order id");
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(service.cancel(actor.userId(), orderId, key));
    }
}
