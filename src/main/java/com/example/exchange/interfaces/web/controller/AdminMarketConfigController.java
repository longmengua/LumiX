/*
 * File purpose: Admin API for inspecting market config and changing audited fee rates.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.FeeConfigAdminService;
import com.example.exchange.domain.model.dto.SymbolConfig;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.interfaces.web.dto.AdminMarketConfigResponse;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.FeeConfigChangeResponse;
import com.example.exchange.interfaces.web.dto.FeeConfigUpdateRequest;
import com.example.exchange.interfaces.web.dto.TradingRuleUpdateRequest;
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
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin/market-config")
@RequiredArgsConstructor
public class AdminMarketConfigController {

    private static final AdminMarketConfigResponse.MarketConfigCapabilities CAPABILITIES =
            new AdminMarketConfigResponse.MarketConfigCapabilities(
                    false,
                    true,
                    List.of("manual-suspension", "session-window-update"),
                    List.of("POST /api/admin/market-config/{symbol}/fees", "POST /api/admin/market-config/{symbol}/trading-rules")
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

    @PostMapping("/{symbol}/trading-rules")
    public ApiResponse<AdminMarketConfigResponse.MarketConfigItem> updateTradingRules(
            @PathVariable String symbol,
            @RequestBody TradingRuleUpdateRequest request
    ) {
        SymbolConfig current = symbolConfigRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("symbol config not found"));
        validatePositive("priceTick", request.priceTick());
        validatePositive("lotSize", request.lotSize());
        validatePositive("minQty", request.minQty());
        validatePositiveOrZero("minNotional", request.minNotional());
        validatePositive("maxOrderNotional", request.maxOrderNotional());
        validatePositiveOrZero("priceBandRate", request.priceBandRate());
        if (request.maxOpenOrders() == null || request.maxOpenOrders() <= 0) {
            throw new IllegalArgumentException("maxOpenOrders must be positive");
        }
        // Trading rules are live pre-trade controls; keep fee/risk-tier and session metadata unchanged.
        SymbolConfig updated = SymbolConfig.builder()
                .symbol(current.getSymbol())
                .baseAsset(current.getBaseAsset())
                .quoteAsset(current.getQuoteAsset())
                .priceTick(request.priceTick())
                .lotSize(request.lotSize())
                .minQty(request.minQty())
                .minNotional(request.minNotional())
                .maxOrderNotional(request.maxOrderNotional())
                .maxPositionNotional(current.getMaxPositionNotional())
                .maxOpenOrders(request.maxOpenOrders())
                .maxLeverage(current.getMaxLeverage())
                .makerFeeRate(current.getMakerFeeRate())
                .takerFeeRate(current.getTakerFeeRate())
                .makerRebateRate(current.getMakerRebateRate())
                .referralRebateRate(current.getReferralRebateRate())
                .priceBandRate(request.priceBandRate())
                .initialMarginRate(current.getInitialMarginRate())
                .maintenanceMarginRate(current.getMaintenanceMarginRate())
                .riskTiers(current.getRiskTiers())
                .tradingEnabled(current.isTradingEnabled())
                .build();
        return ApiResponse.ok(toItem(symbolConfigRepository.save(updated)));
    }

    private static void validatePositive(String field, BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void validatePositiveOrZero(String field, BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(field + " must be zero or positive");
        }
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
