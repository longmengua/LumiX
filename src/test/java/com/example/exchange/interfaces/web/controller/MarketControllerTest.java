/*
 * File purpose: Verify public market metadata API mapping.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.MarketListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MarketControllerTest {

    /**
     * Scenario: repository has enabled and disabled symbols -> client API returns sorted public metadata.
     */
    @Test
    @DisplayName("public market API returns sorted symbols without admin-only fields")
    void returnsPublicMarketRows() {
        MarketController controller = new MarketController(new StubSymbolConfigRepository(List.of(
                config("ETHUSDT", "ETH", "USDT", false),
                config("BTCUSDT", "BTC", "USDT", true)
        )));

        ApiResponse<MarketListResponse> response = controller.list();

        assertThat(response.isOk()).isTrue();
        assertThat(response.getData().markets())
                .extracting(MarketListResponse.MarketItem::symbol)
                .containsExactly("BTCUSDT", "ETHUSDT");
        assertThat(response.getData().markets().get(0).tradingEnabled()).isTrue();
        assertThat(response.getData().markets().get(1).tradingEnabled()).isFalse();
    }

    private SymbolConfig config(String symbol, String base, String quote, boolean tradingEnabled) {
        return SymbolConfig.builder()
                .symbol(symbol)
                .baseAsset(base)
                .quoteAsset(quote)
                .tradingEnabled(tradingEnabled)
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
