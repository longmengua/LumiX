/*
 * File purpose: Read-only admin API for inspecting configured exchange markets.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.interfaces.web.dto.AdminMarketConfigResponse;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
                    true,
                    false,
                    List.of("edit-market-config", "manual-suspension", "session-window-update"),
                    List.of(
                            "POST /api/admin/market-config/{symbol}/changes",
                            "POST /api/admin/market-config/{symbol}/suspension"
                    )
            );

    private final SymbolConfigRepository symbolConfigRepository;

    @GetMapping
    public ApiResponse<AdminMarketConfigResponse> list() {
        List<AdminMarketConfigResponse.MarketConfigItem> markets = symbolConfigRepository.findAll().stream()
                .sorted(Comparator.comparing(SymbolConfig::getSymbol))
                .map(this::toItem)
                .toList();
        return ApiResponse.ok(new AdminMarketConfigResponse(markets, CAPABILITIES));
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
