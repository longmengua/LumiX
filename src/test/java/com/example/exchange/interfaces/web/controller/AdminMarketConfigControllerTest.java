/*
 * File purpose: Verify admin market-config API mapping.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.interfaces.web.dto.AdminMarketConfigResponse;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AdminMarketConfigControllerTest {

    @Test
    @DisplayName("admin market config API returns sorted market rows and fee-write capability")
    /**
     * Scenario: repository has two symbol configs -> admin API returns sorted rows and advertises audited fee writes.
     */
    void returnsMarketConfigRowsWithFeeWriteCapability() {
        AdminMarketConfigController controller =
                new AdminMarketConfigController(new StubSymbolConfigRepository(List.of(
                        config("ETHUSDT", "ETH", "USDT", "0.01", 100),
                        config("BTCUSDT", "BTC", "USDT", "0.10", 125)
                )), null);

        ApiResponse<AdminMarketConfigResponse> response = controller.list();

        assertThat(response.isOk()).isTrue();
        assertThat(response.getData().markets())
                .extracting(AdminMarketConfigResponse.MarketConfigItem::symbol)
                .containsExactly("BTCUSDT", "ETHUSDT");
        assertThat(response.getData().markets().get(0).status()).isEqualTo("TRADING_ENABLED");
        assertThat(response.getData().markets().get(0).matchingEnabled()).isTrue();
        assertThat(response.getData().capabilities().readOnly()).isFalse();
        assertThat(response.getData().capabilities().writesEnabled()).isTrue();
        assertThat(response.getData().capabilities().requiredWriteEndpoints())
                .contains("POST /api/admin/market-config/{symbol}/fees");
    }

    private SymbolConfig config(String symbol, String base, String quote, String priceTick, int leverage) {
        return SymbolConfig.builder()
                .symbol(symbol)
                .baseAsset(base)
                .quoteAsset(quote)
                .priceTick(new BigDecimal(priceTick))
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
