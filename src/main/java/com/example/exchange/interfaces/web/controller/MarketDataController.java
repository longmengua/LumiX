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
import com.example.exchange.domain.model.dto.PerpetualContractSnapshot;
import com.example.exchange.domain.model.dto.PerpetualContractSnapshot;
import com.example.exchange.domain.model.dto.TradeTapeItem;
import com.example.exchange.infra.marketdata.BinanceMarketDataClient;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.security.MarketDataStreamRateLimiter;
import com.example.exchange.interfaces.web.security.UserStreamSubscriptionAuthorizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final BinanceMarketDataClient binanceMarketDataClient;

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
        // Customer-facing reference prices should reflect Binance futures when available; local data is the fallback.
        return ApiResponse.ok(binanceMarketDataClient.ticker(symbol)
                .or(() -> marketDataService.ticker(symbol))
                .orElse(null));
    }

    @GetMapping("/{symbol}/perpetual")
    public ApiResponse<PerpetualContractSnapshot> perpetual(@PathVariable String symbol) {
        // Perpetual UI consumes one contract snapshot; external futures data is preferred, local market data is fallback.
        MarketTicker referenceTicker = binanceMarketDataClient.ticker(symbol)
                .or(() -> marketDataService.ticker(symbol))
                .orElse(null);
        List<MarketKline> referenceKlines = binanceMarketDataClient.klines(symbol, 84);
        if (referenceKlines.isEmpty()) {
            referenceKlines = marketDataService.klines(symbol, 84);
        }
        return ApiResponse.ok(marketDataService.perpetualSnapshot(symbol, referenceTicker, referenceKlines));
    }

    @GetMapping("/{symbol}/trades")
    public ApiResponse<List<TradeTapeItem>> trades(
            @PathVariable String symbol,
            @RequestParam(required = false) Instant afterTs,
            @RequestParam(required = false) String afterMatchId,
            @RequestParam(required = false) Instant beforeTs,
            @RequestParam(required = false) String beforeMatchId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        int normalizedLimit = Math.max(1, Math.min(1000, limit));

        if (beforeTs != null) {
            // before* parameters are used for historical pagination: fetch older trades first.
            return ApiResponse.ok(marketDataService.tradesBefore(symbol, beforeTs, beforeMatchId, normalizedLimit));
        }
        if (afterTs != null) {
            // after* parameters keep compatibility with existing forward cursor usage.
            return ApiResponse.ok(marketDataService.tradesAfter(symbol, afterTs, afterMatchId, normalizedLimit));
        }
        return ApiResponse.ok(marketDataService.trades(symbol, normalizedLimit));
    }

    @GetMapping("/{symbol}/recovery-cursor")
    public ApiResponse<MarketDataRecoveryCursor> recoveryCursor(@PathVariable String symbol) {
        return ApiResponse.ok(marketDataService.recoveryCursor(symbol));
    }

    @GetMapping("/{symbol}/klines")
    public ApiResponse<List<MarketKline>> klines(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam(defaultValue = "100") int limit
    ) {
        // Binance K-lines give the MVP chart a real market shape even before local trade tape is deep enough.
        List<MarketKline> external = binanceMarketDataClient.klines(symbol, interval, limit);
        if (!external.isEmpty()) {
            return ApiResponse.ok(external);
        }
        return ApiResponse.ok(marketDataService.klines(symbol, interval, limit));
    }

    @GetMapping(path = "/{symbol}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter marketStream(@PathVariable String symbol, HttpServletRequest request) {
        enforceAcceptingNewStreams();
        enforceRateLimit(request, "market", symbol);
        return pushGatewayService.subscribeMarket(symbol);
    }

    @GetMapping(path = "/user/{uid}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter userStream(@PathVariable long uid, HttpServletRequest request) {
        enforceAcceptingNewStreams();
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

    private void enforceAcceptingNewStreams() {
        if (!pushGatewayService.acceptingNewStreams()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PUSH_GATEWAY_DRAINING");
        }
    }
}
