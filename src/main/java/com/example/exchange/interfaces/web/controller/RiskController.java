package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.command.LiquidateCommand;
import com.example.exchange.application.service.FundingRateService;
import com.example.exchange.application.service.InsuranceFundService;
import com.example.exchange.application.usecase.LiquidateUseCase;
import com.example.exchange.domain.model.dto.AdlQueueEntry;
import com.example.exchange.domain.model.dto.FundingSettlementResult;
import com.example.exchange.domain.model.dto.LiquidationResult;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.FundingSettlementRequest;
import com.example.exchange.interfaces.web.dto.LiquidationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final FundingRateService fundingRateService;
    private final LiquidateUseCase liquidateUseCase;
    private final InsuranceFundService insuranceFundService;

    @PostMapping("/funding/settle")
    public ApiResponse<FundingSettlementResult> settleFunding(
            @Valid @RequestBody FundingSettlementRequest request
    ) {
        return ApiResponse.ok(fundingRateService.settle(
                request.uid(),
                request.symbol(),
                request.markPrice(),
                request.fundingRate()
        ));
    }

    @PostMapping("/liquidate")
    public ApiResponse<LiquidationResult> liquidate(@Valid @RequestBody LiquidationRequest request) {
        return ApiResponse.ok(liquidateUseCase.handle(new LiquidateCommand(
                request.uid(),
                request.symbol(),
                request.markPrice()
        )));
    }

    @GetMapping("/insurance-fund")
    public ApiResponse<BigDecimal> insuranceFund(@RequestParam(defaultValue = "USDT") String asset) {
        return ApiResponse.ok(insuranceFundService.balance(asset));
    }

    @GetMapping("/adl-queue")
    public ApiResponse<List<AdlQueueEntry>> adlQueue() {
        return ApiResponse.ok(insuranceFundService.adlQueue());
    }
}
