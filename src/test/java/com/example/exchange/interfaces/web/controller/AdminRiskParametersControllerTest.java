/*
 * File purpose: Verify read-only admin risk-parameter API mapping.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.MarkPriceOracleService;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.infra.config.MarkPriceOracleProperties;
import com.example.exchange.infra.config.RiskControlsProperties;
import com.example.exchange.interfaces.web.dto.AdminRiskParametersResponse;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AdminRiskParametersControllerTest {

    @Test
    @DisplayName("admin risk parameters API returns switches, tiers, and oracle states")
    /**
     * Scenario: one suspended symbol has oracle input and one active symbol is missing oracle input.
     */
    void returnsRiskSwitchesTiersAndOracleStates() {
        RiskControlsProperties riskControls = new RiskControlsProperties();
        riskControls.setLiquidationHalt(true);
        riskControls.setSuspendedSymbols(List.of("BTCUSDT"));
        riskControls.getOrderEntryFrequencyLimit().setEnabled(true);
        riskControls.getOrderEntryFrequencyLimit().setMaxOrders(12);

        MarkPriceOracleService oracleService =
                new MarkPriceOracleService(new MarkPriceOracleProperties());
        oracleService.update("BTCUSDT", new BigDecimal("60000"), new BigDecimal("60010"), "test");

        AdminRiskParametersController controller = new AdminRiskParametersController(
                new StubSymbolConfigRepository(List.of(
                        config("ETHUSDT", 100),
                        config("BTCUSDT", 125)
                )),
                riskControls,
                oracleService
        );

        ApiResponse<AdminRiskParametersResponse> response = controller.list();

        assertThat(response.isOk()).isTrue();
        assertThat(response.getData().switches().liquidationHalt()).isTrue();
        assertThat(response.getData().switches().suspendedSymbols()).containsExactly("BTCUSDT");
        assertThat(response.getData().switches().orderEntryFrequencyLimitEnabled()).isTrue();

        AdminRiskParametersResponse.RiskSymbolParameter btc = response.getData().symbols().get(0);
        AdminRiskParametersResponse.RiskSymbolParameter eth = response.getData().symbols().get(1);
        assertThat(btc.symbol()).isEqualTo("BTCUSDT");
        assertThat(btc.status()).isEqualTo("SUSPENDED");
        assertThat(btc.oracle().status()).isEqualTo("FRESH");
        assertThat(btc.riskTiers()).hasSize(1);
        assertThat(eth.symbol()).isEqualTo("ETHUSDT");
        assertThat(eth.oracle().status()).isEqualTo("MISSING");
        assertThat(response.getData().capabilities().writesEnabled()).isFalse();
    }

    private SymbolConfig config(String symbol, int leverage) {
        return SymbolConfig.builder()
                .symbol(symbol)
                .baseAsset(symbol.substring(0, 3))
                .quoteAsset("USDT")
                .priceTick(new BigDecimal("0.01"))
                .lotSize(new BigDecimal("0.001"))
                .minQty(new BigDecimal("0.001"))
                .minNotional(new BigDecimal("5"))
                .maxOrderNotional(new BigDecimal("100000"))
                .maxPositionNotional(new BigDecimal("500000"))
                .maxOpenOrders(200)
                .maxLeverage(leverage)
                .makerFeeRate(new BigDecimal("0.0002"))
                .takerFeeRate(new BigDecimal("0.0005"))
                .priceBandRate(new BigDecimal("0.10"))
                .initialMarginRate(new BigDecimal("0.01"))
                .maintenanceMarginRate(new BigDecimal("0.005"))
                .riskTiers(List.of(SymbolConfig.RiskTier.builder()
                        .tier(1)
                        .maxPositionNotional(new BigDecimal("500000"))
                        .initialMarginRate(new BigDecimal("0.01"))
                        .maintenanceMarginRate(new BigDecimal("0.005"))
                        .maxLeverage(leverage)
                        .build()))
                .tradingEnabled(true)
                .build();
    }

    private record StubSymbolConfigRepository(List<SymbolConfig> configs) implements SymbolConfigRepository {
        @Override
        public Optional<SymbolConfig> findBySymbol(String symbol) {
            return configs.stream()
                    .filter(config -> config.getSymbol().equals(symbol))
                    .findFirst();
        }

        @Override
        public List<SymbolConfig> findAll() {
            return configs;
        }
    }
}
