package com.lumix.trading.runtime;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 只從已驗證 session 取得 owner；每個命令與資金變更在單一後端交易完成。HUMAN_REVIEW_REQUIRED。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/trading/spot/orders")
public class SpotTradingCommandController {
    private final SpotTradingExecutionService service;
    private final TradingCommandSecurity security;
    private final ObjectMapper json;
    public SpotTradingCommandController(SpotTradingExecutionService service,TradingCommandSecurity security,ObjectMapper json) {
        this.service=service;this.security=security;
        // 交易數值只能使用 JSON string，禁止 Jackson 將浮點數或布林值暗中轉成價格字串。
        this.json=json.copy().disable(com.fasterxml.jackson.databind.MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.json.coercionConfigFor(com.fasterxml.jackson.databind.type.LogicalType.Textual)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Integer,com.fasterxml.jackson.databind.cfg.CoercionAction.Fail)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Float,com.fasterxml.jackson.databind.cfg.CoercionAction.Fail)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Boolean,com.fasterxml.jackson.databind.cfg.CoercionAction.Fail);
    }

    /** 先限定 body 大小再反序列化，不讓無界 JSON 在接單 gate 前消耗記憶體。 */
    @PostMapping(consumes="application/json")
    public ResponseEntity<TradingOrderQueryService.OrderView> place(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @RequestHeader("Idempotency-Key") String key,HttpServletRequest request) throws IOException {
        security.require(request,true,"spot");
        byte[] body=request.getInputStream().readNBytes(4097);
        if(body.length>4096)throw new IllegalArgumentException("order command exceeds size limit");
        var command=json.readValue(body,SpotTradingExecutionService.Command.class);
        return ResponseEntity.ok().header("Cache-Control","no-store").body(service.place(user.userId(),command,key));
    }

    /** DELETE 亦需來源及冪等；關閉新單時不阻擋剩餘委託釋放。 */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<TradingOrderQueryService.OrderView> cancel(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable String orderId,@RequestHeader("Idempotency-Key") String key,HttpServletRequest request) {
        security.require(request,false,"spot");
        if(orderId.length()>64)throw new IllegalArgumentException("invalid order id");
        return ResponseEntity.ok().header("Cache-Control","no-store").body(service.cancel(user.userId(),orderId,key));
    }
}
