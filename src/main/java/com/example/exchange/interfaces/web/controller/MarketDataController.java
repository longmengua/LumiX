package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.MarketDataService;
import com.example.exchange.domain.model.dto.DepthDelta;
import com.example.exchange.domain.model.dto.MarketKline;
import com.example.exchange.domain.model.dto.MarketTicker;
import com.example.exchange.domain.model.dto.TradeTapeItem;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;

    @GetMapping("/{symbol}/depth-delta")
    public ApiResponse<DepthDelta> depthDelta(@PathVariable String symbol) {
        return ApiResponse.ok(marketDataService.latestDepthDelta(symbol).orElse(null));
    }

    @GetMapping("/{symbol}/ticker")
    public ApiResponse<MarketTicker> ticker(@PathVariable String symbol) {
        return ApiResponse.ok(marketDataService.ticker(symbol).orElse(null));
    }

    @GetMapping("/{symbol}/trades")
    public ApiResponse<List<TradeTapeItem>> trades(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ApiResponse.ok(marketDataService.trades(symbol, limit));
    }

    @GetMapping("/{symbol}/klines")
    public ApiResponse<List<MarketKline>> klines(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ApiResponse.ok(marketDataService.klines(symbol, limit));
    }
}
