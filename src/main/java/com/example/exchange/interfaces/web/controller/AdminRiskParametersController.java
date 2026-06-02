/*
 * File purpose: Read-only admin API for inspecting risk parameters and risk switches.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.MarkPriceOracleService;
import com.example.exchange.domain.model.dto.MarkPriceSnapshot;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.infra.config.RiskControlsProperties;
import com.example.exchange.interfaces.web.dto.AdminRiskParametersResponse;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/risk-parameters")
@RequiredArgsConstructor
public class AdminRiskParametersController {

    private static final AdminRiskParametersResponse.RiskParameterCapabilities CAPABILITIES =
            new AdminRiskParametersResponse.RiskParameterCapabilities(
                    true,
                    false,
                    List.of("risk-switch-update", "symbol-suspension-update", "risk-tier-update"),
                    List.of(
                            "POST /api/admin/risk-parameters/switches",
                            "POST /api/admin/risk-parameters/{symbol}/suspension",
                            "POST /api/admin/risk-parameters/{symbol}/tiers"
                    )
            );

    private final SymbolConfigRepository symbolConfigRepository;
    private final RiskControlsProperties riskControlsProperties;
    private final MarkPriceOracleService markPriceOracleService;

    @GetMapping
    public ApiResponse<AdminRiskParametersResponse> list() {
        Set<String> suspendedSymbols = riskControlsProperties.getSuspendedSymbols().stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(symbol -> symbol.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<AdminRiskParametersResponse.RiskSymbolParameter> symbols = symbolConfigRepository.findAll().stream()
                .sorted(Comparator.comparing(SymbolConfig::getSymbol))
                .map(config -> toSymbolParameter(config, suspendedSymbols))
                .toList();

        return ApiResponse.ok(new AdminRiskParametersResponse(
                switches(suspendedSymbols),
                symbols,
                CAPABILITIES
        ));
    }

    private AdminRiskParametersResponse.RiskSwitches switches(Set<String> suspendedSymbols) {
        RiskControlsProperties.OrderEntryFrequencyLimit frequencyLimit =
                riskControlsProperties.getOrderEntryFrequencyLimit();
        return new AdminRiskParametersResponse.RiskSwitches(
                riskControlsProperties.isOrderEntryHalt(),
                riskControlsProperties.isReduceOnlyMode(),
                riskControlsProperties.isWithdrawalHalt(),
                riskControlsProperties.isLiquidationHalt(),
                riskControlsProperties.isLiquidationManualReview(),
                riskControlsProperties.getLiquidationScanBatchSize(),
                riskControlsProperties.isMarketMakerHedgeExecutionHalt(),
                suspendedSymbols.stream().sorted().toList(),
                frequencyLimit.isEnabled(),
                frequencyLimit.getMaxOrders(),
                frequencyLimit.getWindowSeconds()
        );
    }

    private AdminRiskParametersResponse.RiskSymbolParameter toSymbolParameter(
            SymbolConfig config,
            Set<String> suspendedSymbols
    ) {
        boolean suspended = suspendedSymbols.contains(config.getSymbol().toUpperCase(Locale.ROOT));
        return new AdminRiskParametersResponse.RiskSymbolParameter(
                config.getSymbol(),
                suspended ? "SUSPENDED" : (config.isTradingEnabled() ? "ACTIVE" : "TRADING_DISABLED"),
                suspended,
                config.maxLeverageOrDefault(),
                config.maxOrderNotionalOrDefault(),
                config.maxPositionNotionalOrDefault(),
                config.initialMarginRateOrDefault(),
                config.maintenanceMarginRateOrDefault(),
                config.priceBandRateOrDefault(),
                oracle(config.getSymbol()),
                riskTiers(config)
        );
    }

    private AdminRiskParametersResponse.OracleState oracle(String symbol) {
        return markPriceOracleService.snapshot(symbol)
                .map(this::toOracleState)
                .orElseGet(() -> new AdminRiskParametersResponse.OracleState(
                        "MISSING",
                        null,
                        null,
                        null,
                        null,
                        true
                ));
    }

    private AdminRiskParametersResponse.OracleState toOracleState(MarkPriceSnapshot snapshot) {
        return new AdminRiskParametersResponse.OracleState(
                snapshot.stale() ? "STALE" : "FRESH",
                snapshot.markPrice(),
                snapshot.indexPrice(),
                snapshot.source(),
                snapshot.updatedAt(),
                snapshot.stale()
        );
    }

    private List<AdminRiskParametersResponse.RiskTierParameter> riskTiers(SymbolConfig config) {
        if (config.getRiskTiers() == null || config.getRiskTiers().isEmpty()) {
            return List.of(toRiskTier(config.riskTierForNotional(config.maxPositionNotionalOrDefault())));
        }
        return config.getRiskTiers().stream()
                .map(this::toRiskTier)
                .toList();
    }

    private AdminRiskParametersResponse.RiskTierParameter toRiskTier(SymbolConfig.RiskTier tier) {
        return new AdminRiskParametersResponse.RiskTierParameter(
                tier.getTier(),
                tier.getMaxPositionNotional(),
                tier.getInitialMarginRate(),
                tier.getMaintenanceMarginRate(),
                tier.getMaxLeverage()
        );
    }
}
