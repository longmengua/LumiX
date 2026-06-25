/*
 * File purpose: Verify public market metadata API mapping.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.domain.model.dto.SymbolConfig;
import com.example.exchange.domain.model.enums.ProductType;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.MarketListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketControllerTest {

    /**
     * Scenario: repository has enabled and disabled symbols -> client API returns sorted public metadata.
     */
    @Test
    @DisplayName("public market API returns sorted symbols without admin-only fields")
    void returnsPublicMarketRows() {
        MarketController controller = new MarketController(new StubSymbolConfigRepository(List.of(
                config("ETHUSDT", "ETH", "USDT", false, ProductType.SPOT),
                config("BTCUSDT", "BTC", "USDT", true, ProductType.PERPETUAL)
        )));

        ApiResponse<MarketListResponse> response = controller.list();

        assertThat(response.isOk()).isTrue();
        assertThat(response.getData().markets())
                .extracting(MarketListResponse.MarketItem::symbol)
                .containsExactly("BTCUSDT", "ETHUSDT");
        assertThat(response.getData().markets().get(0).tradingEnabled()).isTrue();
        assertThat(response.getData().markets().get(0).productType()).isEqualTo("PERPETUAL");
        assertThat(response.getData().markets().get(1).tradingEnabled()).isFalse();
    }

    /**
     * Scenario: productType filter narrows the public market list and symbol lookup normalizes input.
     */
    @Test
    @DisplayName("public market API filters by productType and finds a normalized symbol")
    void filtersByProductTypeAndFindsSymbol() {
        MarketController controller = new MarketController(new StubSymbolConfigRepository(List.of(
                config("ETHUSDT-SPOT", "ETH", "USDT", true, ProductType.SPOT),
                config("BTCUSDT-PERP", "BTC", "USDT", true, ProductType.PERPETUAL)
        )));

        ApiResponse<MarketListResponse> spotResponse = controller.list(" spot ");
        ApiResponse<MarketListResponse.MarketItem> singleResponse = controller.get(" btcusdt-perp ");

        assertThat(spotResponse.isOk()).isTrue();
        assertThat(spotResponse.getData().markets())
                .extracting(MarketListResponse.MarketItem::symbol)
                .containsExactly("ETHUSDT-SPOT");
        assertThat(singleResponse.isOk()).isTrue();
        assertThat(singleResponse.getData().symbol()).isEqualTo("BTCUSDT-PERP");
    }

    /**
     * Scenario: missing symbols should return 404 so callers can distinguish not-found from validation errors.
     */
    @Test
    @DisplayName("public market API returns 404 for unknown symbol")
    void missingSymbolReturns404() {
        MarketController controller = new MarketController(new StubSymbolConfigRepository(List.of()));

        assertThatThrownBy(() -> controller.get("does-not-exist"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("symbol config not found");
    }

    private SymbolConfig config(String symbol, String base, String quote, boolean tradingEnabled, ProductType productType) {
        return SymbolConfig.builder()
                .symbol(symbol)
                .baseAsset(base)
                .quoteAsset(quote)
                .productType(productType)
                .tradingEnabled(tradingEnabled)
                .build();
    }

    private record StubSymbolConfigRepository(List<SymbolConfig> configs) implements SymbolConfigRepository {
        @Override
        public Optional<SymbolConfig> findBySymbol(String symbol) {
            return configs.stream()
                    .filter(config -> config.getSymbol().equals(symbol == null ? null : symbol.trim().toUpperCase()))
                    .findFirst();
        }

        @Override
        public List<SymbolConfig> findAll() {
            return configs;
        }
    }
}
