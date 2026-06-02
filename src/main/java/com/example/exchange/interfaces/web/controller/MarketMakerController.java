/*
 * 檔案用途：REST Controller，提供做市商 profile/risk-limit、hedge fill 與 execution 後台 API。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.MarketMakerHedgeFillService;
import com.example.exchange.application.service.HedgeVenueCallbackVerifier;
import com.example.exchange.application.service.MarketMakerHedgeExecutionService;
import com.example.exchange.application.service.MarketMakerHedgeReconciliationService;
import com.example.exchange.application.service.MarketMakerHedgeVenueIdempotencyService;
import com.example.exchange.application.service.MarketMakerProfileService;
import com.example.exchange.application.service.MarketMakerQuoteLifecycleService;
import com.example.exchange.application.service.MarketMakerQuoteReconciliationService;
import com.example.exchange.domain.model.dto.HedgeFillRecord;
import com.example.exchange.domain.model.dto.HedgeExecutionReport;
import com.example.exchange.domain.model.dto.HedgeReconciliationReport;
import com.example.exchange.domain.model.dto.HedgeVenueIdempotencyReport;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerQuoteLifecycleReport;
import com.example.exchange.domain.model.dto.MarketMakerQuoteReconciliationReport;
import com.example.exchange.domain.model.dto.MarketMakerQuoteRepairReport;
import com.example.exchange.domain.model.dto.MarketMakerQuoteState;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.HedgeVenueFillCallbackRequest;
import com.example.exchange.interfaces.web.dto.MarketMakerProfileRequest;
import com.example.exchange.interfaces.web.dto.MarketMakerQuoteRequest;
import com.example.exchange.interfaces.web.security.MarketMakerHedgeExecutionRateLimiter;
import com.example.exchange.interfaces.web.security.MarketMakerQuoteRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/market-maker")
@RequiredArgsConstructor
public class MarketMakerController {

    private final MarketMakerProfileService profileService;
    private final MarketMakerHedgeFillService hedgeFillService;
    private final MarketMakerHedgeReconciliationService hedgeReconciliationService;
    private final MarketMakerHedgeExecutionService hedgeExecutionService;
    private final MarketMakerHedgeVenueIdempotencyService hedgeVenueIdempotencyService;
    private final HedgeVenueCallbackVerifier hedgeVenueCallbackVerifier;
    private final MarketMakerQuoteLifecycleService quoteLifecycleService;
    private final MarketMakerQuoteReconciliationService quoteReconciliationService;
    private final MarketMakerQuoteRateLimiter marketMakerQuoteRateLimiter;
    private final MarketMakerHedgeExecutionRateLimiter marketMakerHedgeExecutionRateLimiter;

    @PostMapping("/profiles")
    public ApiResponse<MarketMakerProfile> saveProfile(
            @Valid @RequestBody MarketMakerProfileRequest request
    ) {
        return ApiResponse.ok(profileService.save(request.toProfile()));
    }

    @GetMapping("/profiles/enabled")
    public ApiResponse<List<MarketMakerProfile>> enabledProfiles() {
        return ApiResponse.ok(profileService.enabledProfiles());
    }

    @GetMapping("/profiles/{marketMakerId}")
    public ApiResponse<MarketMakerProfile> profile(@PathVariable String marketMakerId) {
        return ApiResponse.ok(profileService.findByMarketMakerId(marketMakerId).orElse(null));
    }

    @GetMapping("/uids/{uid}/profile")
    public ApiResponse<MarketMakerProfile> profileByUid(@PathVariable long uid) {
        return ApiResponse.ok(profileService.findByUid(uid).orElse(null));
    }

    @PostMapping("/quotes")
    public ApiResponse<MarketMakerQuoteLifecycleReport> placeQuote(
            @Valid @RequestBody MarketMakerQuoteRequest request,
            HttpServletRequest httpRequest
    ) {
        enforceQuoteRateLimit(httpRequest, request);
        return ApiResponse.ok(quoteLifecycleService.placeQuote(request.toCommand()));
    }

    @GetMapping("/quotes/active")
    public ApiResponse<List<MarketMakerQuoteState>> activeQuoteStates(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(quoteLifecycleService.activeQuoteStates(limit));
    }

    @GetMapping("/profiles/{marketMakerId}/quotes")
    public ApiResponse<List<MarketMakerQuoteState>> quoteStatesByMarketMaker(
            @PathVariable String marketMakerId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(quoteLifecycleService.quoteStatesByMarketMaker(marketMakerId, limit));
    }

    @GetMapping("/profiles/{marketMakerId}/quotes/{symbol}")
    public ApiResponse<MarketMakerQuoteState> quoteState(
            @PathVariable String marketMakerId,
            @PathVariable String symbol
    ) {
        return ApiResponse.ok(quoteLifecycleService.quoteState(marketMakerId, symbol).orElse(null));
    }

    @GetMapping("/quotes/reconciliation")
    public ApiResponse<MarketMakerQuoteReconciliationReport> reconcileQuoteStates(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(quoteReconciliationService.reconcileActiveQuotes(limit));
    }

    @PostMapping("/quotes/reconciliation/repair")
    public ApiResponse<MarketMakerQuoteRepairReport> repairQuoteStates(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(quoteReconciliationService.repairActiveQuotes(limit));
    }

    @GetMapping("/profiles/{marketMakerId}/hedge-fills")
    public ApiResponse<List<HedgeFillRecord>> hedgeFillsByMarketMaker(
            @PathVariable String marketMakerId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(hedgeFillService.fillsByMarketMaker(marketMakerId, limit));
    }

    @GetMapping("/hedge-fills/venue-orders/{venueOrderId}")
    public ApiResponse<List<HedgeFillRecord>> hedgeFillsByVenueOrder(@PathVariable String venueOrderId) {
        return ApiResponse.ok(hedgeFillService.fillsByVenueOrder(venueOrderId));
    }

    @GetMapping("/hedge-fills/ref/{refId}")
    public ApiResponse<List<HedgeFillRecord>> hedgeFillsByRefId(@PathVariable String refId) {
        return ApiResponse.ok(hedgeFillService.fillsByRefId(refId));
    }

    @PostMapping("/hedge-fills/venue-callback")
    public ApiResponse<HedgeFillRecord> recordHedgeFillCallback(
            @Valid @RequestBody HedgeVenueFillCallbackRequest request,
            @RequestHeader(value = "X-Hedge-Venue-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Hedge-Venue-Signature", required = false) String signature
    ) {
        hedgeVenueCallbackVerifier.verify(request, timestamp, signature);
        return ApiResponse.ok(hedgeFillService.recordVenueFill(request.toMessage()));
    }

    @GetMapping("/profiles/{marketMakerId}/hedge-reconciliation")
    public ApiResponse<HedgeReconciliationReport> hedgeReconciliationByMarketMaker(
            @PathVariable String marketMakerId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(hedgeReconciliationService.reconcileMarketMaker(marketMakerId, limit));
    }

    @GetMapping("/hedge-idempotency/unresolved")
    public ApiResponse<HedgeVenueIdempotencyReport> unresolvedHedgeVenueIdempotency(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(hedgeVenueIdempotencyService.unresolved(limit));
    }

    @PostMapping("/hedge-idempotency/reconcile")
    public ApiResponse<HedgeVenueIdempotencyReport> reconcileHedgeVenueIdempotency(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(hedgeVenueIdempotencyService.reconcileUnresolved(limit));
    }

    @PostMapping("/profiles/{marketMakerId}/hedge-execution")
    public ApiResponse<HedgeExecutionReport> executeHedgeByMarketMaker(
            @PathVariable String marketMakerId,
            @RequestParam(defaultValue = "manual") String refPrefix,
            @RequestHeader(value = "X-Operator-Approval", required = false) String approvalToken,
            HttpServletRequest httpRequest
    ) {
        enforceHedgeExecutionRateLimit(httpRequest, marketMakerId);
        return ApiResponse.ok(hedgeExecutionService.executeForMarketMaker(marketMakerId, refPrefix, approvalToken));
    }

    @PostMapping("/hedge-execution/enabled")
    public ApiResponse<List<HedgeExecutionReport>> executeHedgeForEnabledMarketMakers(
            @RequestParam(defaultValue = "manual") String refPrefix,
            @RequestHeader(value = "X-Operator-Approval", required = false) String approvalToken,
            HttpServletRequest httpRequest
    ) {
        enforceHedgeExecutionRateLimit(
                httpRequest,
                MarketMakerHedgeExecutionRateLimiter.ENABLED_MARKET_MAKERS_SCOPE
        );
        return ApiResponse.ok(hedgeExecutionService.executeForEnabledMarketMakers(refPrefix, approvalToken));
    }

    private void enforceQuoteRateLimit(HttpServletRequest httpRequest, MarketMakerQuoteRequest request) {
        MarketMakerQuoteRateLimiter.RateLimitDecision decision =
                marketMakerQuoteRateLimiter.consume(httpRequest, request);
        if (!decision.allowed()) {
            throw new ResponseStatusException(decision.status(), decision.reason());
        }
    }

    private void enforceHedgeExecutionRateLimit(HttpServletRequest httpRequest, String executionScope) {
        MarketMakerHedgeExecutionRateLimiter.RateLimitDecision decision =
                marketMakerHedgeExecutionRateLimiter.consume(httpRequest, executionScope);
        if (!decision.allowed()) {
            throw new ResponseStatusException(decision.status(), decision.reason());
        }
    }
}
