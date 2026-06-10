/*
 * File purpose: Admin API for inspecting market config and changing audited fee rates.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.FeeConfigAdminService;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.interfaces.web.dto.AdminMarketConfigResponse;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.FeeConfigChangeResponse;
import com.example.exchange.interfaces.web.dto.FeeConfigUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/admin/market-config")
@RequiredArgsConstructor
public class AdminMarketConfigController {

    private static final AdminMarketConfigResponse.MarketConfigCapabilities CAPABILITIES =
            new AdminMarketConfigResponse.MarketConfigCapabilities(
                    false,
                    true,
                    List.of("edit-core-limits", "manual-suspension", "session-window-update"),
                    List.of("POST /api/admin/market-config/{symbol}/fees")
            );

    private final SymbolConfigRepository symbolConfigRepository;
    private final FeeConfigAdminService feeConfigAdminService;

    @GetMapping
    public ApiResponse<AdminMarketConfigResponse> list() {
        List<AdminMarketConfigResponse.MarketConfigItem> markets = symbolConfigRepository.findAll().stream()
                .sorted(Comparator.comparing(SymbolConfig::getSymbol))
                .map(this::toItem)
                .toList();
        return ApiResponse.ok(new AdminMarketConfigResponse(markets, CAPABILITIES));
    }

    @PostMapping("/{symbol}/fees")
    public ApiResponse<FeeConfigChangeResponse> updateFees(
            @PathVariable String symbol,
            @RequestBody FeeConfigUpdateRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        // Fee updates are audited and only affect new orders because existing orders carry fee snapshots.
        return ApiResponse.ok(FeeConfigChangeResponse.from(feeConfigAdminService.updateFees(
                symbol,
                request.makerFeeRate(),
                request.takerFeeRate(),
                request.operatorId(),
                request.reason(),
                requestId,
                request.effectiveAt()
        )));
    }

    @GetMapping("/{symbol}/fees/changes")
    public ApiResponse<List<FeeConfigChangeResponse>> recentFeeChanges(@PathVariable String symbol) {
        return ApiResponse.ok(feeConfigAdminService.recentChanges(symbol).stream()
                .map(FeeConfigChangeResponse::from)
                .toList());
    }

    private AdminMarketConfigResponse.MarketConfigItem toItem(SymbolConfig config) {
        return new AdminMarketConfigResponse.MarketConfigItem(
                config.getSymbol(),
                config.getBaseAsset(),
                config.getQuoteAsset(),
                config.isTradingEnabled() ? "TRADING_ENABLED" : "TRADING_DISABLED",
                config.priceTickOrDefault(),
                config.lotSizeOrDefault(),
                config.minQtyOrDefault(),
                config.minNotionalOrDefault(),
                config.maxOrderNotionalOrDefault(),
                config.maxPositionNotionalOrDefault(),
                config.maxOpenOrdersOrDefault(),
                config.maxLeverageOrDefault(),
                config.makerFeeRateOrDefault(),
                config.takerFeeRateOrDefault(),
                config.priceBandRateOrDefault(),
                config.initialMarginRateOrDefault(),
                config.maintenanceMarginRateOrDefault(),
                "PERPETUAL",
                "ALWAYS_OPEN",
                config.isTradingEnabled(),
                true,
                !config.isTradingEnabled(),
                riskTiers(config)
        );
    }

    private List<AdminMarketConfigResponse.RiskTierItem> riskTiers(SymbolConfig config) {
        if (config.getRiskTiers() == null || config.getRiskTiers().isEmpty()) {
            SymbolConfig.RiskTier fallback = config.riskTierForNotional(config.maxPositionNotionalOrDefault());
            return List.of(toRiskTierItem(fallback));
        }
        return config.getRiskTiers().stream()
                .map(this::toRiskTierItem)
                .toList();
    }

    private AdminMarketConfigResponse.RiskTierItem toRiskTierItem(SymbolConfig.RiskTier tier) {
        return new AdminMarketConfigResponse.RiskTierItem(
                tier.getTier(),
                tier.getMaxPositionNotional(),
                tier.getInitialMarginRate(),
                tier.getMaintenanceMarginRate(),
                tier.getMaxLeverage()
        );
    }
}
