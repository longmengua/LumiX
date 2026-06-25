/*
 * 檔案用途：驗證後台 market-config API 會正確讀寫單一 symbol，並保留不相關欄位。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.FeeConfigAdminService;
import com.example.exchange.domain.model.dto.SymbolConfig;
import com.example.exchange.domain.model.enums.ProductType;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.interfaces.web.dto.AdminMarketConfigResponse;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.TradingRuleUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

class AdminMarketConfigControllerTest {

    /**
     * Scenario: admin can fetch a single symbol and the response should expose the true product type.
     */
    @Test
    @DisplayName("admin market-config get returns a normalized symbol with actual product type")
    void getReturnsNormalizedSymbol() {
        InMemorySymbolConfigRepository repository = new InMemorySymbolConfigRepository(config(
                " btcusdt-spot ",
                ProductType.SPOT,
                "BTC",
                "USDT",
                "USDT",
                true,
                false
        ));
        AdminMarketConfigController controller = new AdminMarketConfigController(repository, mock(FeeConfigAdminService.class));

        ApiResponse<AdminMarketConfigResponse.MarketConfigItem> response = controller.get(" btcusdt-spot ");

        assertThat(response.isOk()).isTrue();
        assertThat(response.getData().symbol()).isEqualTo("BTCUSDT-SPOT");
        assertThat(response.getData().tradingMode()).isEqualTo("SPOT");
    }

    /**
     * Scenario: missing admin symbol lookups should fail with 404 instead of a generic validation error.
     */
    @Test
    @DisplayName("admin market-config get returns 404 for unknown symbol")
    void getReturns404ForUnknownSymbol() {
        AdminMarketConfigController controller = new AdminMarketConfigController(
                new InMemorySymbolConfigRepository(),
                mock(FeeConfigAdminService.class)
        );

        Throwable thrown = catchThrowable(() -> controller.get("unknown"));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Scenario: trading-rule updates should only touch pre-trade rule fields and keep product/risk metadata intact.
     */
    @Test
    @DisplayName("trading-rules update preserves non-trading fields and uses actual product type")
    void updateTradingRulesPreservesUnrelatedFields() {
        InMemorySymbolConfigRepository repository = new InMemorySymbolConfigRepository(config(
                "BTCUSDT-PERP",
                ProductType.PERPETUAL,
                "BTC",
                "USDT",
                "USDT",
                true,
                true
        ));
        AdminMarketConfigController controller = new AdminMarketConfigController(repository, mock(FeeConfigAdminService.class));

        TradingRuleUpdateRequest request = new TradingRuleUpdateRequest(
                new BigDecimal("0.25"),
                new BigDecimal("0.002"),
                new BigDecimal("0.010"),
                new BigDecimal("8"),
                new BigDecimal("500000"),
                250,
                new BigDecimal("0.12")
        );

        ApiResponse<AdminMarketConfigResponse.MarketConfigItem> response = controller.updateTradingRules(" btcusdt-perp ", request);
        SymbolConfig saved = repository.findBySymbol("BTCUSDT-PERP").orElseThrow();

        assertThat(response.isOk()).isTrue();
        assertThat(response.getData().tradingMode()).isEqualTo("PERPETUAL");
        assertThat(saved.productTypeOrDefault()).isEqualTo(ProductType.PERPETUAL);
        assertThat(saved.getBaseAsset()).isEqualTo("BTC");
        assertThat(saved.getQuoteAsset()).isEqualTo("USDT");
        assertThat(saved.getMarginAsset()).isEqualTo("USDT");
        assertThat(saved.getMaxPositionNotional()).isEqualByComparingTo("2500000");
        assertThat(saved.getMaxLeverage()).isEqualTo(125);
        assertThat(saved.getInitialMarginRate()).isEqualByComparingTo("0.008");
        assertThat(saved.getMaintenanceMarginRate()).isEqualByComparingTo("0.004");
        assertThat(saved.getRiskTiers()).hasSize(1);
        assertThat(saved.isTradingEnabled()).isTrue();
        assertThat(saved.getVisible()).isTrue();
        assertThat(saved.getReduceOnly()).isFalse();
        assertThat(saved.getPriceTick()).isEqualByComparingTo("0.25");
        assertThat(saved.getLotSize()).isEqualByComparingTo("0.002");
        assertThat(saved.getMinQty()).isEqualByComparingTo("0.010");
        assertThat(saved.getMinNotional()).isEqualByComparingTo("8");
        assertThat(saved.getMaxOrderNotional()).isEqualByComparingTo("500000");
        assertThat(saved.getMaxOpenOrders()).isEqualTo(250);
        assertThat(saved.getPriceBandRate()).isEqualByComparingTo("0.12");
    }

    private static SymbolConfig config(
            String symbol,
            ProductType productType,
            String baseAsset,
            String quoteAsset,
            String marginAsset,
            boolean tradingEnabled,
            boolean visible
    ) {
        return SymbolConfig.builder()
                .symbol(symbol)
                .productType(productType)
                .baseAsset(baseAsset)
                .quoteAsset(quoteAsset)
                .marginAsset(marginAsset)
                .priceTick(new BigDecimal("0.10"))
                .lotSize(new BigDecimal("0.001"))
                .minQty(new BigDecimal("0.001"))
                .minNotional(new BigDecimal("5"))
                .maxOrderNotional(new BigDecimal("1000000"))
                .maxPositionNotional(new BigDecimal("2500000"))
                .maxOpenOrders(125)
                .maxLeverage(125)
                .makerFeeRate(new BigDecimal("0.0002"))
                .takerFeeRate(new BigDecimal("0.0005"))
                .makerRebateRate(BigDecimal.ZERO)
                .referralRebateRate(BigDecimal.ZERO)
                .priceBandRate(new BigDecimal("0.10"))
                .initialMarginRate(new BigDecimal("0.008"))
                .maintenanceMarginRate(new BigDecimal("0.004"))
                .riskTiers(List.of(SymbolConfig.RiskTier.builder()
                        .tier(1)
                        .maxPositionNotional(new BigDecimal("2500000"))
                        .initialMarginRate(new BigDecimal("0.008"))
                        .maintenanceMarginRate(new BigDecimal("0.004"))
                        .maxLeverage(125)
                        .build()))
                .tradingEnabled(tradingEnabled)
                .visible(visible)
                .reduceOnly(false)
                .build();
    }

    private static final class InMemorySymbolConfigRepository implements SymbolConfigRepository {
        private final Map<String, SymbolConfig> configs = new LinkedHashMap<>();

        private InMemorySymbolConfigRepository(SymbolConfig... initialConfigs) {
            for (SymbolConfig config : initialConfigs) {
                save(config);
            }
        }

        @Override
        public Optional<SymbolConfig> findBySymbol(String symbol) {
            return Optional.ofNullable(configs.get(normalize(symbol)));
        }

        @Override
        public List<SymbolConfig> findAll() {
            return List.copyOf(configs.values());
        }

        @Override
        public SymbolConfig save(SymbolConfig config) {
            config.setSymbol(normalize(config.getSymbol()));
            configs.put(normalize(config.getSymbol()), config);
            return config;
        }

        private static String normalize(String symbol) {
            return symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        }
    }
}
