/*
 * 檔案用途：REST app 啟動時重建 in-memory order book，避免 open orders 與 depth 查詢分裂。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MatchingEngineSnapshot;
import com.example.exchange.domain.model.dto.Order;
import com.example.exchange.domain.model.dto.SymbolConfig;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.domain.service.MatchingEngine;
import com.example.exchange.infra.config.MatchingBookRecoveryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchingBookRecoveryService {

    private final MatchingBookRecoveryProperties properties;
    private final MatchingRecoveryService durableRecoveryService;
    private final MatchingEngine matchingEngine;
    private final OrderRepository orderRepository;
    private final SymbolConfigRepository symbolConfigRepository;

    public List<MatchingBookRecoveryResult> recoverConfiguredBooks() {
        if (!properties.isEnabled()) {
            return List.of();
        }
        return configuredSymbols().stream()
                .map(this::recoverSymbol)
                .toList();
    }

    public MatchingBookRecoveryResult recoverSymbol(String symbolCode) {
        String symbol = normalize(symbolCode);
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("matching book recovery symbol is required");
        }

        // Durable replay is the preferred path because it preserves sequencer offsets and command history.
        var durableResult = durableRecoveryService.recoverSymbol(symbol);
        MatchingEngineSnapshot afterDurable = matchingEngine.exportSnapshot(symbol);
        int durableRestingOrders = restingOrderCount(afterDurable);
        if (durableRestingOrders > 0 || !properties.isOpenOrderFallbackEnabled()) {
            return new MatchingBookRecoveryResult(symbol, true, durableRestingOrders, 0, "DURABLE_REPLAY");
        }

        List<Order> openOrders = orderRepository.openOrders().stream()
                .filter(order -> isOpenForSymbol(order, symbol))
                .sorted(Comparator.comparing(Order::getCtime))
                .toList();
        if (openOrders.isEmpty()) {
            return new MatchingBookRecoveryResult(symbol, durableResult.recovered(), 0, 0, "EMPTY");
        }

        MatchingEngineSnapshot fallbackSnapshot = new MatchingEngineSnapshot(
                symbol,
                afterDurable.matchSequence(),
                afterDurable.commandOffset(),
                afterDurable.eventOffset(),
                openOrders.stream().filter(order -> order.getSide() == OrderSide.BUY).toList(),
                openOrders.stream().filter(order -> order.getSide() == OrderSide.SELL).toList(),
                Instant.now()
        );
        // Restore uses copied orders inside the engine, so persisted repository objects are not mutated by book state.
        matchingEngine.restoreSnapshot(fallbackSnapshot);
        return new MatchingBookRecoveryResult(symbol, true, 0, openOrders.size(), "OPEN_ORDER_FALLBACK");
    }

    private List<String> configuredSymbols() {
        Set<String> symbols = new LinkedHashSet<>();
        properties.getSymbols().stream()
                .map(MatchingBookRecoveryService::normalize)
                .filter(symbol -> !symbol.isBlank())
                .forEach(symbols::add);
        if (symbols.isEmpty()) {
            symbolConfigRepository.findAll().stream()
                    .map(SymbolConfig::getSymbol)
                    .map(MatchingBookRecoveryService::normalize)
                    .filter(symbol -> !symbol.isBlank())
                    .forEach(symbols::add);
        }
        return List.copyOf(symbols);
    }

    private static boolean isOpenForSymbol(Order order, String symbol) {
        if (order == null || order.getSymbol() == null || order.getSymbol().code() == null) {
            return false;
        }
        return symbol.equals(normalize(order.getSymbol().code()))
                && (order.getStatus() == Order.Status.NEW || order.getStatus() == Order.Status.PARTIALLY_FILLED)
                && order.getQty() != null
                && order.getQty().signum() > 0;
    }

    private static int restingOrderCount(MatchingEngineSnapshot snapshot) {
        if (snapshot == null) {
            return 0;
        }
        return snapshot.bids().size() + snapshot.asks().size();
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }

    public record MatchingBookRecoveryResult(
            String symbol,
            boolean recovered,
            int durableRestingOrders,
            int fallbackRestoredOrders,
            String source
    ) {
    }
}
