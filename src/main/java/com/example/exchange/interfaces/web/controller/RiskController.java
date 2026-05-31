/*
 * 檔案用途：REST Controller，暴露 HTTP API 並委派給應用或領域服務。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.command.LiquidateCommand;
import com.example.exchange.application.service.AdlForcedExecutionService;
import com.example.exchange.application.service.AdlInsuranceReconciliationService;
import com.example.exchange.application.service.AdlQueueExecutionService;
import com.example.exchange.application.service.FundingRateService;
import com.example.exchange.application.service.InsuranceFundService;
import com.example.exchange.application.service.MarkPriceOracleService;
import com.example.exchange.application.usecase.LiquidateUseCase;
import com.example.exchange.domain.model.dto.AdlInsuranceReconciliationReport;
import com.example.exchange.domain.model.dto.AdlExecutionResult;
import com.example.exchange.domain.model.dto.AdlQueueEntry;
import com.example.exchange.domain.model.dto.FundingSettlementResult;
import com.example.exchange.domain.model.dto.LiquidationResult;
import com.example.exchange.domain.model.dto.MarkPriceSnapshot;
import com.example.exchange.interfaces.web.dto.AdlQueueClaimRequest;
import com.example.exchange.interfaces.web.dto.AdlQueueExecutionRequest;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.FundingSettlementRequest;
import com.example.exchange.interfaces.web.dto.LiquidationRequest;
import com.example.exchange.interfaces.web.dto.MarkPriceUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final FundingRateService fundingRateService;
    private final LiquidateUseCase liquidateUseCase;
    private final InsuranceFundService insuranceFundService;
    private final MarkPriceOracleService markPriceOracleService;
    private final AdlQueueExecutionService adlQueueExecutionService;
    private final AdlForcedExecutionService adlForcedExecutionService;
    private final AdlInsuranceReconciliationService adlInsuranceReconciliationService;

    @PostMapping("/funding/settle")
    public ApiResponse<FundingSettlementResult> settleFunding(
            @Valid @RequestBody FundingSettlementRequest request
    ) {
        return ApiResponse.ok(fundingRateService.settle(
                request.uid(),
                request.symbol(),
                request.fundingRate()
        ));
    }

    @PostMapping("/liquidate")
    public ApiResponse<LiquidationResult> liquidate(@Valid @RequestBody LiquidationRequest request) {
        return ApiResponse.ok(liquidateUseCase.handle(new LiquidateCommand(
                request.uid(),
                request.symbol(),
                null
        )));
    }

    @PutMapping("/price-oracle")
    public ApiResponse<MarkPriceSnapshot> updatePriceOracle(
            @Valid @RequestBody MarkPriceUpdateRequest request
    ) {
        return ApiResponse.ok(markPriceOracleService.update(
                request.symbol(),
                request.markPrice(),
                request.indexPrice(),
                request.source()
        ));
    }

    @GetMapping("/price-oracle/{symbol}")
    public ApiResponse<MarkPriceSnapshot> priceOracle(@PathVariable String symbol) {
        return ApiResponse.ok(markPriceOracleService.snapshot(symbol).orElse(null));
    }

    @GetMapping("/insurance-fund")
    public ApiResponse<BigDecimal> insuranceFund(@RequestParam(defaultValue = "USDT") String asset) {
        return ApiResponse.ok(insuranceFundService.balance(asset));
    }

    @GetMapping("/adl-queue")
    public ApiResponse<List<AdlQueueEntry>> adlQueue() {
        return ApiResponse.ok(insuranceFundService.adlQueue());
    }

    @GetMapping("/adl-queue/stuck-claims")
    public ApiResponse<List<AdlQueueEntry>> stuckAdlClaims(
            @RequestParam(defaultValue = "900") long minClaimAgeSeconds
    ) {
        long safeSeconds = Math.max(0, minClaimAgeSeconds);
        return ApiResponse.ok(insuranceFundService.stuckAdlClaims(Duration.ofSeconds(safeSeconds)));
    }

    @GetMapping("/adl-executions")
    public ApiResponse<List<AdlExecutionResult>> recentAdlExecutions(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(adlForcedExecutionService.recentExecutions(limit));
    }

    @GetMapping("/adl-insurance-reconciliation")
    public ApiResponse<AdlInsuranceReconciliationReport> adlInsuranceReconciliation(
            @RequestParam(defaultValue = "USDT") String asset
    ) {
        return ApiResponse.ok(adlInsuranceReconciliationService.reconcile(asset));
    }

    @PostMapping("/adl-queue/{liquidationId}/execute")
    public ApiResponse<AdlExecutionResult> executeAdlQueueEntry(
            @PathVariable String liquidationId,
            @Valid @RequestBody AdlQueueExecutionRequest request
    ) {
        return ApiResponse.ok(adlQueueExecutionService.execute(request.commandId(), liquidationId, request.operatorId()));
    }

    @PostMapping("/adl-queue/{liquidationId}/claim")
    public ApiResponse<AdlQueueEntry> claimAdlQueueEntry(
            @PathVariable String liquidationId,
            @Valid @RequestBody AdlQueueClaimRequest request
    ) {
        return ApiResponse.ok(insuranceFundService.claimAdl(liquidationId, request.owner()));
    }

    @PostMapping("/adl-queue/{liquidationId}/release")
    public ApiResponse<AdlQueueEntry> releaseAdlQueueEntry(
            @PathVariable String liquidationId,
            @Valid @RequestBody AdlQueueClaimRequest request
    ) {
        return ApiResponse.ok(insuranceFundService.releaseAdl(liquidationId, request.owner()));
    }
}
