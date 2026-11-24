package com.lumix.trading.liquidity;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 已登入使用者可直接呼叫的外部送單 Demo API；回應不會進入真實 order 或 ledger。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/trading/external-proxy/demo")
public class DemoExternalOrderController {
    private final DemoExternalOrderRouter demo;
    private final ExternalOrderProxy proxy;
    private final ExternalOrderProxySettingsService settings;
    private final String allowedOrigin;

    public DemoExternalOrderController(DemoExternalOrderRouter demo, ExternalOrderProxy proxy,
            ExternalOrderProxySettingsService settings,
            @Value("${lumix.trading.allowed-origin:}") String allowedOrigin) {
        this.demo = demo;
        this.proxy = proxy;
        this.settings = settings;
        this.allowedOrigin = allowedOrigin;
    }

    /** 回傳可用情境，便於測試端選擇或以 RANDOM 隨機演練。 */
    @GetMapping("/scenarios")
    public ResponseEntity<List<String>> scenarios(HttpServletRequest request) {
        requireLocalDemoRequest(request);
        settings.requireDemoEnabled();
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(List.of("RANDOM", "NORMAL", "DELAYED", "PARTIAL", "SLIPPAGE_FILLED",
                        "SLIPPAGE_REJECTED", "VENUE_REJECTED", "UNKNOWN_TIMEOUT"));
    }

    /** 只接受小型、純數值 Demo 請求，固定 source 為 DEMO 且只回傳模擬狀態。 */
    @PostMapping
    public ResponseEntity<ExternalOrderRouter.Outcome> submit(
            @RequestBody Request body, HttpServletRequest request) {
        requireLocalDemoRequest(request);
        if (body == null || body.clientOrderId() == null || body.clientOrderId().length() > 96)
            throw new IllegalArgumentException("demo order request is invalid");
        var intent = new ExternalOrderRouter.Intent("DEMO", body.clientOrderId(), body.kind(), body.marketSymbol(),
                body.side(), body.quantity(), body.referencePrice(), body.limitPrice(), body.timeInForce());
        var outcome = body.scenario() == null || "RANDOM".equals(body.scenario())
                ? proxy.submit(intent)
                : demo.submit(intent, DemoExternalOrderRouter.Scenario.valueOf(body.scenario()));
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(outcome);
    }

    /** 供延遲情境以原 clientOrderId 查狀態；未進入交易帳本。 */
    @GetMapping("/{clientOrderId}")
    public ResponseEntity<ExternalOrderRouter.Outcome> lookup(
            @PathVariable String clientOrderId, HttpServletRequest request) {
        requireLocalDemoRequest(request);
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(proxy.lookup("DEMO", clientOrderId));
    }

    private void requireLocalDemoRequest(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String remote = request.getRemoteAddr();
        if (allowedOrigin.isBlank() || !allowedOrigin.equals(origin)
                || !("127.0.0.1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote) || "::1".equals(remote)))
            throw new IllegalArgumentException("demo request origin is invalid");
    }

    public record Request(String clientOrderId, String scenario, com.lumix.trading.runtime.TradingInstrumentRules.Kind kind,
                          String marketSymbol, ExternalOrderRouter.Side side, BigDecimal quantity,
                          BigDecimal referencePrice, BigDecimal limitPrice,
                          ExternalOrderRouter.TimeInForce timeInForce) { }
}
