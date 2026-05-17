/*
 * 檔案用途：REST Controller，暴露 HTTP API 並委派給應用或領域服務。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.domain.model.dto.TopOfBook;
import com.example.exchange.domain.service.MatchingEngine;
import com.example.exchange.domain.service.OrderBookSnapshot;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.DepthResponse;
import org.springframework.web.bind.annotation.*;

/**
 * 訂單簿深度查詢 API
 *
 * GET /api/depth/{symbol}?depth=10
 */
@RestController
@RequestMapping("/api/depth")
public class DepthController {

    private final MatchingEngine matchingEngine;

    public DepthController(MatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
    }

    @GetMapping("/{symbol}")
    public ApiResponse<DepthResponse> depth(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "10") int depth
    ) {
        // 取得快照
        OrderBookSnapshot snap = matchingEngine.snapshot(symbol, depth);

        // 取得最優價
        var top = matchingEngine.top(symbol).orElseGet(() -> TopOfBook.builder().build());

        // 轉成回應 DTO
        DepthResponse res = new DepthResponse(
                symbol,
                top.getBestBid(),
                top.getBestAsk(),
                snap.bids().stream()
                        .map(l -> new DepthResponse.Level(l.price(), l.qty()))
                        .toList(),
                snap.asks().stream()
                        .map(l -> new DepthResponse.Level(l.price(), l.qty()))
                        .toList()
        );

        return ApiResponse.ok(res);
    }
}
