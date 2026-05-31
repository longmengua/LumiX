/*
 * 檔案用途：對帳 durable active quote state 與 order repository 內的實際 open quote orders。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MarketMakerQuoteReconciliationIssue;
import com.example.exchange.domain.model.dto.MarketMakerQuoteReconciliationReport;
import com.example.exchange.domain.model.dto.MarketMakerQuoteState;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.repository.MarketMakerQuoteStateStore;
import com.example.exchange.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketMakerQuoteReconciliationService {

    private static final int MAX_QUERY_LIMIT = 500;

    private final MarketMakerQuoteStateStore quoteStateStore;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public MarketMakerQuoteReconciliationReport reconcileActiveQuotes(int limit) {
        validateLimit(limit);
        List<MarketMakerQuoteState> states = quoteStateStore.findActive(limit);
        List<MarketMakerQuoteReconciliationIssue> issues = new ArrayList<>();
        for (MarketMakerQuoteState state : states) {
            issues.addAll(reconcileState(state));
        }
        return new MarketMakerQuoteReconciliationReport(
                states.size(),
                issues.size(),
                Instant.now(),
                issues
        );
    }

    private List<MarketMakerQuoteReconciliationIssue> reconcileState(MarketMakerQuoteState state) {
        List<Order> openOrders = orderRepository.findOpenOrders(state.uid(), state.symbol());
        Set<UUID> openOrderIds = openOrders.stream()
                .map(Order::getId)
                .collect(Collectors.toSet());
        List<MarketMakerQuoteReconciliationIssue> issues = new ArrayList<>();

        addMissingExpectedOrder(issues, state, "BUY", state.bidOrderId(), openOrderIds);
        addMissingExpectedOrder(issues, state, "SELL", state.askOrderId(), openOrderIds);

        Set<UUID> expectedOrderIds = expectedOrderIds(state);
        for (Order order : openOrders) {
            if (order.getClientOrderId() == null || !order.getClientOrderId().startsWith(quotePrefix(state))) {
                continue;
            }
            if (!expectedOrderIds.contains(order.getId())) {
                issues.add(issue(state, order.getSide().name(), order.getId(), order.getClientOrderId(), "UNTRACKED_OPEN_QUOTE_ORDER"));
            }
        }
        return issues;
    }

    private static void addMissingExpectedOrder(
            List<MarketMakerQuoteReconciliationIssue> issues,
            MarketMakerQuoteState state,
            String side,
            UUID expectedOrderId,
            Set<UUID> openOrderIds
    ) {
        if (expectedOrderId == null) {
            issues.add(issue(state, side, null, null, "MISSING_TRACKED_ORDER_ID"));
            return;
        }
        if (!openOrderIds.contains(expectedOrderId)) {
            issues.add(issue(state, side, expectedOrderId, null, "TRACKED_ORDER_NOT_OPEN"));
        }
    }

    private static Set<UUID> expectedOrderIds(MarketMakerQuoteState state) {
        return java.util.stream.Stream.of(state.bidOrderId(), state.askOrderId())
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    private static MarketMakerQuoteReconciliationIssue issue(
            MarketMakerQuoteState state,
            String side,
            UUID orderId,
            String clientOrderId,
            String reason
    ) {
        return new MarketMakerQuoteReconciliationIssue(
                state.marketMakerId(),
                state.uid(),
                state.symbol(),
                side,
                orderId,
                clientOrderId,
                reason
        );
    }

    private static String quotePrefix(MarketMakerQuoteState state) {
        return "mmq:" + state.marketMakerId().trim() + ":";
    }

    private static void validateLimit(int limit) {
        if (limit <= 0 || limit > MAX_QUERY_LIMIT) {
            throw new IllegalArgumentException("quote reconciliation query limit must be between 1 and " + MAX_QUERY_LIMIT);
        }
    }
}
