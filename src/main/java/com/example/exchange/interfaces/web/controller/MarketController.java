/*
 * File purpose: Public market metadata API for client trading surfaces.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.domain.model.dto.SymbolConfig;
import com.example.exchange.domain.model.enums.ProductType;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.MarketListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/markets")
@RequiredArgsConstructor
public class MarketController {

    private final SymbolConfigRepository symbolConfigRepository;

    @GetMapping
    public ApiResponse<MarketListResponse> list() {
        return list(null);
    }

    @GetMapping(params = "productType")
    public ApiResponse<MarketListResponse> list(@RequestParam(required = false) String productType) {
        ProductType filter = parseProductType(productType);
        List<MarketListResponse.MarketItem> markets = symbolConfigRepository.findAll().stream()
                .filter(config -> filter == null || config.productTypeOrDefault() == filter)
                .sorted(Comparator.comparing(SymbolConfig::getSymbol))
                .map(this::toItem)
                .toList();
        return ApiResponse.ok(new MarketListResponse(markets));
    }

    @GetMapping("/{symbol}")
    public ApiResponse<MarketListResponse.MarketItem> get(@PathVariable String symbol) {
        SymbolConfig config = symbolConfigRepository.findBySymbol(normalizeSymbol(symbol))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "symbol config not found"));
        return ApiResponse.ok(toItem(config));
    }

    private MarketListResponse.MarketItem toItem(SymbolConfig config) {
        return new MarketListResponse.MarketItem(
                config.getSymbol(),
                config.productTypeOrDefault().name(),
                config.getBaseAsset(),
                config.getQuoteAsset(),
                config.isTradingEnabled(),
                config.priceTickOrDefault(),
                config.lotSizeOrDefault(),
                config.minQtyOrDefault(),
                config.minNotionalOrDefault()
        );
    }

    /**
     * Accepts optional productType filters without forcing the caller to know our internal casing.
     */
    private static ProductType parseProductType(String productType) {
        if (productType == null || productType.isBlank()) {
            return null;
        }
        try {
            return ProductType.valueOf(productType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid productType");
        }
    }

    /**
     * Normalize symbol lookups so trim/upper-case matching is consistent across APIs.
     */
    private static String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
    }
}
