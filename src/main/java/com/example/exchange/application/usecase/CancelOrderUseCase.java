/*
 * 檔案用途：UseCase 入口，承接介面層請求並協調應用服務執行業務動作。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.application.service.CommandTransactionBoundary;
import com.example.exchange.application.service.MarketDataService;
import com.example.exchange.application.service.MatchingWorkerExecutionService;
import com.example.exchange.application.service.MatchingWorkerLifecycleService;
import com.example.exchange.application.service.OperationalMetricsService;
import com.example.exchange.application.service.WalletLedgerService;
import com.example.exchange.domain.event.OrderLifecycleEvent;
import com.example.exchange.domain.model.dto.MatchingResult;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.domain.service.MatchingEngine;
import com.example.exchange.domain.service.OrderBookSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final SymbolConfigRepository symbolConfigRepository;
    private final MatchingEngine matchingEngine;
    private final WalletLedgerService walletLedgerService;
    private final MarketDataService marketDataService;
    private final DomainEventPublisher<Object> publisher;
    private OperationalMetricsService operationalMetricsService;
    private CommandTransactionBoundary commandTransactionBoundary;
    private MatchingWorkerLifecycleService matchingWorkerLifecycleService;
    private MatchingWorkerExecutionService matchingWorkerExecutionService;

    @Autowired(required = false)
    public void setOperationalMetricsService(OperationalMetricsService operationalMetricsService) {
        this.operationalMetricsService = operationalMetricsService;
    }

    @Autowired(required = false)
    public void setCommandTransactionBoundary(CommandTransactionBoundary commandTransactionBoundary) {
        this.commandTransactionBoundary = commandTransactionBoundary;
    }

    @Autowired(required = false)
    public void setMatchingWorkerLifecycleService(MatchingWorkerLifecycleService matchingWorkerLifecycleService) {
        this.matchingWorkerLifecycleService = matchingWorkerLifecycleService;
    }

    @Autowired(required = false)
    public void setMatchingWorkerExecutionService(MatchingWorkerExecutionService matchingWorkerExecutionService) {
        this.matchingWorkerExecutionService = matchingWorkerExecutionService;
    }

    public boolean handle(UUID orderId) {
        if (commandTransactionBoundary != null) {
            return commandTransactionBoundary.execute("cancel-order", () -> handleInsideTransaction(orderId));
        }
        return handleInsideTransaction(orderId);
    }

    private boolean handleInsideTransaction(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));
        if (order.getStatus() != Order.Status.NEW && order.getStatus() != Order.Status.PARTIALLY_FILLED) {
            return false;
        }

        boolean removed = cancelThroughConfiguredMatchingPath(order);
        if (!removed) return false;

        order.cancel();
        releaseReserve(order);
        orderRepository.save(order);
        publisher.publish(OrderLifecycleEvent.canceled(order));

        OrderBookSnapshot snapshot = matchingEngine.snapshot(order.getSymbol().code(), 50);
        marketDataService.onOrderBookChanged(
                order.getSymbol().code(),
                snapshot,
                matchingEngine.top(order.getSymbol().code())
        );
        if (operationalMetricsService != null) {
            operationalMetricsService.recordCanceledOrders(1);
        }
        return true;
    }

    public int cancelOpenOrders(long uid, String symbol) {
        if (commandTransactionBoundary != null) {
            return commandTransactionBoundary.execute("bulk-cancel-order", () -> cancelOpenOrdersInsideTransaction(uid, symbol));
        }
        return cancelOpenOrdersInsideTransaction(uid, symbol);
    }

    private int cancelOpenOrdersInsideTransaction(long uid, String symbol) {
        List<Order> openOrders = (symbol == null || symbol.isBlank())
                ? orderRepository.openOrders(uid)
                : orderRepository.findOpenOrders(uid, symbol);
        int canceled = 0;
        Set<String> affectedSymbols = new LinkedHashSet<>();

        for (Order order : openOrders) {
            if (order.getStatus() != Order.Status.NEW && order.getStatus() != Order.Status.PARTIALLY_FILLED) {
                continue;
            }
            boolean removed = cancelThroughConfiguredMatchingPath(order);
            if (!removed) continue;

            order.cancel();
            releaseReserve(order);
            orderRepository.save(order);
            publisher.publish(OrderLifecycleEvent.canceled(order));
            affectedSymbols.add(order.getSymbol().code());
            canceled++;
        }

        for (String affectedSymbol : affectedSymbols) {
            OrderBookSnapshot snapshot = matchingEngine.snapshot(affectedSymbol, 50);
            marketDataService.onOrderBookChanged(
                    affectedSymbol,
                    snapshot,
                    matchingEngine.top(affectedSymbol)
            );
        }
        if (operationalMetricsService != null) {
            operationalMetricsService.recordCanceledOrders(canceled);
        }
        return canceled;
    }

    private void releaseReserve(Order order) {
        BigDecimal reserved = order.getReservedAmount();
        if (reserved == null || reserved.signum() <= 0) return;
        SymbolConfig config = symbolConfigRepository.findBySymbol(order.getSymbol().code())
                .orElseThrow(() -> new IllegalArgumentException("missing symbol config: " + order.getSymbol().code()));
        walletLedgerService.releaseOrderReserve(
                order.getUid(),
                order.getReservedAsset() == null ? config.getQuoteAsset() : order.getReservedAsset(),
                reserved,
                order.getId().toString()
        );
        order.setReservedAmount(BigDecimal.ZERO);
        order.setReservedAsset(null);
    }

    private boolean cancelThroughConfiguredMatchingPath(Order order) {
        Optional<MatchingWorkerLifecycleService.MatchingWorkerOwnerContext> context = workerContext(order);
        if (context.isEmpty()) {
            return matchingEngine.cancelOrder(order);
        }
        MatchingWorkerLifecycleService.MatchingWorkerOwnerContext owner = context.get();
        MatchingResult result = matchingWorkerExecutionService.cancel(order, owner.ownerId(), owner.ownerEpoch());
        return result.getAffectedOrders() != null && !result.getAffectedOrders().isEmpty();
    }

    private Optional<MatchingWorkerLifecycleService.MatchingWorkerOwnerContext> workerContext(Order order) {
        if (matchingWorkerLifecycleService == null || matchingWorkerExecutionService == null
                || order == null || order.getSymbol() == null) {
            return Optional.empty();
        }
        return matchingWorkerLifecycleService.routingOwnerContext(order.getSymbol().code());
    }
}
