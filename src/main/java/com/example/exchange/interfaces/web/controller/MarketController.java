/*
 * File purpose: Public market metadata API for client trading surfaces.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.domain.model.dto.SymbolConfig;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.MarketListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/markets")
@RequiredArgsConstructor
public class MarketController {

    private final SymbolConfigRepository symbolConfigRepository;

    @GetMapping
    public ApiResponse<MarketListResponse> list() {
        List<MarketListResponse.MarketItem> markets = symbolConfigRepository.findAll().stream()
                .sorted(Comparator.comparing(SymbolConfig::getSymbol))
                .map(this::toItem)
                .toList();
        return ApiResponse.ok(new MarketListResponse(markets));
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
}
