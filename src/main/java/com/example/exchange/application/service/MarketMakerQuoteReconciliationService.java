/*
 * 檔案用途：對帳 durable active quote state 與 order repository 內的實際 open quote orders。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MarketMakerQuoteReconciliationIssue;
import com.example.exchange.domain.model.dto.MarketMakerQuoteReconciliationReport;
import com.example.exchange.domain.model.dto.MarketMakerQuoteRepairAction;
import com.example.exchange.domain.model.dto.MarketMakerQuoteRepairReport;
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
    private final MarketMakerQuoteOrderGateway quoteOrderGateway;

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

    @Transactional
    public MarketMakerQuoteRepairReport repairActiveQuotes(int limit) {
        validateLimit(limit);
        List<MarketMakerQuoteState> states = quoteStateStore.findActive(limit);
        List<MarketMakerQuoteRepairAction> actions = new ArrayList<>();
        int issueCount = 0;
        int canceledOrders = 0;
        int deactivatedStates = 0;

        for (MarketMakerQuoteState state : states) {
            RepairStateResult result = repairState(state);
            issueCount += result.issueCount();
            canceledOrders += result.canceledOrders();
            deactivatedStates += result.deactivated() ? 1 : 0;
            actions.addAll(result.actions());
        }

        return new MarketMakerQuoteRepairReport(
                states.size(),
                issueCount,
                canceledOrders,
                deactivatedStates,
                Instant.now(),
                actions
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

    private RepairStateResult repairState(MarketMakerQuoteState state) {
        List<Order> openOrders = orderRepository.findOpenOrders(state.uid(), state.symbol());
        Set<UUID> openOrderIds = openOrders.stream()
                .map(Order::getId)
                .collect(Collectors.toSet());
        List<MarketMakerQuoteReconciliationIssue> issues = new ArrayList<>();
        addMissingExpectedOrder(issues, state, "BUY", state.bidOrderId(), openOrderIds);
        addMissingExpectedOrder(issues, state, "SELL", state.askOrderId(), openOrderIds);

        Set<UUID> expectedOrderIds = expectedOrderIds(state);
        List<MarketMakerQuoteRepairAction> actions = new ArrayList<>();
        int canceledOrders = 0;
        boolean missingTrackedOrder = issues.stream()
                .anyMatch(issue -> "MISSING_TRACKED_ORDER_ID".equals(issue.reason())
                        || "TRACKED_ORDER_NOT_OPEN".equals(issue.reason()));

        for (Order order : openOrders) {
            if (!isMarketMakerQuoteOrder(state, order) || expectedOrderIds.contains(order.getId())) {
                continue;
            }
            issues.add(issue(state, order.getSide().name(), order.getId(), order.getClientOrderId(), "UNTRACKED_OPEN_QUOTE_ORDER"));
            if (cancelQuoteOrder(order.getId())) {
                canceledOrders++;
                actions.add(action(state, order, "CANCEL_UNTRACKED_OPEN_QUOTE_ORDER", "UNTRACKED_OPEN_QUOTE_ORDER", true));
            } else {
                actions.add(action(state, order, "CANCEL_UNTRACKED_OPEN_QUOTE_ORDER", "UNTRACKED_OPEN_QUOTE_ORDER", false));
            }
        }

        if (missingTrackedOrder) {
            // A one-sided market-maker quote is unsafe after restart; fail closed and require the next quote command to rebuild both legs.
            for (Order order : openOrders) {
                if (!expectedOrderIds.contains(order.getId())) {
                    continue;
                }
                if (cancelQuoteOrder(order.getId())) {
                    canceledOrders++;
                    actions.add(action(state, order, "CANCEL_REMAINING_TRACKED_QUOTE_ORDER", "TRACKED_QUOTE_STATE_INCOMPLETE", true));
                } else {
                    actions.add(action(state, order, "CANCEL_REMAINING_TRACKED_QUOTE_ORDER", "TRACKED_QUOTE_STATE_INCOMPLETE", false));
                }
            }
            quoteStateStore.save(inactiveState(state, "QUOTE_REPAIR_DEACTIVATED_INCOMPLETE_STATE"));
            actions.add(new MarketMakerQuoteRepairAction(
                    state.marketMakerId(),
                    state.uid(),
                    state.symbol(),
                    "STATE",
                    null,
                    null,
                    "DEACTIVATE_QUOTE_STATE",
                    "TRACKED_QUOTE_STATE_INCOMPLETE",
                    true
            ));
        }

        return new RepairStateResult(issues.size(), canceledOrders, missingTrackedOrder, actions);
    }

    private boolean cancelQuoteOrder(UUID orderId) {
        return orderId != null && quoteOrderGateway.cancelOrder(orderId);
    }

    private static boolean isMarketMakerQuoteOrder(MarketMakerQuoteState state, Order order) {
        return order.getClientOrderId() != null && order.getClientOrderId().startsWith(quotePrefix(state));
    }

    private static MarketMakerQuoteRepairAction action(
            MarketMakerQuoteState state,
            Order order,
            String action,
            String reason,
            boolean success
    ) {
        return new MarketMakerQuoteRepairAction(
                state.marketMakerId(),
                state.uid(),
                state.symbol(),
                order.getSide().name(),
                order.getId(),
                order.getClientOrderId(),
                action,
                reason,
                success
        );
    }

    private static MarketMakerQuoteState inactiveState(MarketMakerQuoteState state, String reason) {
        return new MarketMakerQuoteState(
                state.marketMakerId(),
                state.uid(),
                state.symbol(),
                state.refId(),
                false,
                false,
                reason,
                state.canceledCount(),
                state.bidPrice(),
                state.bidQuantity(),
                state.askPrice(),
                state.askQuantity(),
                state.bidOrderId(),
                state.askOrderId(),
                state.bidVersion(),
                state.askVersion(),
                state.replacedBidOrderId(),
                state.replacedAskOrderId(),
                Instant.now()
        );
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

    private record RepairStateResult(
            int issueCount,
            int canceledOrders,
            boolean deactivated,
            List<MarketMakerQuoteRepairAction> actions
    ) {
    }
}
