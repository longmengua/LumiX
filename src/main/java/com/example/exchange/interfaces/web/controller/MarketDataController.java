/*
 * 檔案用途：REST Controller，暴露 HTTP API 並委派給應用或領域服務。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.MarketDataService;
import com.example.exchange.application.service.PushGatewayService;
import com.example.exchange.domain.model.dto.DepthDelta;
import com.example.exchange.domain.model.dto.MarketKline;
import com.example.exchange.domain.model.dto.MarketDataRecoveryCursor;
import com.example.exchange.domain.model.dto.MarketTicker;
import com.example.exchange.domain.model.dto.TradeTapeItem;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.security.MarketDataStreamRateLimiter;
import com.example.exchange.interfaces.web.security.UserStreamSubscriptionAuthorizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.time.Instant;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final PushGatewayService pushGatewayService;
    private final MarketDataStreamRateLimiter marketDataStreamRateLimiter;
    private final UserStreamSubscriptionAuthorizer userStreamSubscriptionAuthorizer;

    @GetMapping("/{symbol}/depth-delta")
    public ApiResponse<DepthDelta> depthDelta(@PathVariable String symbol) {
        return ApiResponse.ok(marketDataService.latestDepthDelta(symbol).orElse(null));
    }

    @GetMapping("/{symbol}/depth-deltas")
    public ApiResponse<List<DepthDelta>> depthDeltas(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "0") long afterVersion,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ApiResponse.ok(marketDataService.depthDeltasAfter(symbol, afterVersion, limit));
    }

    @GetMapping("/{symbol}/ticker")
    public ApiResponse<MarketTicker> ticker(@PathVariable String symbol) {
        return ApiResponse.ok(marketDataService.ticker(symbol).orElse(null));
    }

    @GetMapping("/{symbol}/trades")
    public ApiResponse<List<TradeTapeItem>> trades(
            @PathVariable String symbol,
            @RequestParam(required = false) Instant afterTs,
            @RequestParam(required = false) String afterMatchId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        if (afterTs != null) {
            return ApiResponse.ok(marketDataService.tradesAfter(symbol, afterTs, afterMatchId, limit));
        }
        return ApiResponse.ok(marketDataService.trades(symbol, limit));
    }

    @GetMapping("/{symbol}/recovery-cursor")
    public ApiResponse<MarketDataRecoveryCursor> recoveryCursor(@PathVariable String symbol) {
        return ApiResponse.ok(marketDataService.recoveryCursor(symbol));
    }

    @GetMapping("/{symbol}/klines")
    public ApiResponse<List<MarketKline>> klines(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ApiResponse.ok(marketDataService.klines(symbol, limit));
    }

    @GetMapping(path = "/{symbol}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter marketStream(@PathVariable String symbol, HttpServletRequest request) {
        enforceRateLimit(request, "market", symbol);
        return pushGatewayService.subscribeMarket(symbol);
    }

    @GetMapping(path = "/user/{uid}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter userStream(@PathVariable long uid, HttpServletRequest request) {
        enforceRateLimit(request, "user", Long.toString(uid));
        UserStreamSubscriptionAuthorizer.UserStreamAuthorizationDecision decision =
                userStreamSubscriptionAuthorizer.authorize(
                        uid,
                        request.getHeader(userStreamSubscriptionAuthorizer.apiKeyHeaderName()),
                        request.getHeader("Authorization")
                );
        if (!decision.allowed()) {
            throw new ResponseStatusException(decision.status(), decision.reason());
        }
        return pushGatewayService.subscribeUser(uid);
    }

    private void enforceRateLimit(HttpServletRequest request, String streamType, String streamId) {
        MarketDataStreamRateLimiter.RateLimitDecision decision =
                marketDataStreamRateLimiter.consume(request, streamType, streamId);
        if (!decision.allowed()) {
            throw new ResponseStatusException(decision.status(), decision.reason());
        }
    }
}
