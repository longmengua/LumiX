/*
 * 檔案用途：UseCase 入口，承接介面層請求並協調應用服務執行業務動作。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.application.service.MarketDataService;
import com.example.exchange.application.service.WalletLedgerService;
import com.example.exchange.domain.event.OrderLifecycleEvent;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.domain.service.MatchingEngine;
import com.example.exchange.domain.service.OrderBookSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

    public boolean handle(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));
        if (order.getStatus() != Order.Status.NEW && order.getStatus() != Order.Status.PARTIALLY_FILLED) {
            return false;
        }

        boolean removed = matchingEngine.cancelOrder(order);
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
        return true;
    }

    private void releaseReserve(Order order) {
        BigDecimal reserved = order.getReservedAmount();
        if (reserved == null || reserved.signum() <= 0) return;
        SymbolConfig config = symbolConfigRepository.findBySymbol(order.getSymbol().code())
                .orElseThrow(() -> new IllegalArgumentException("missing symbol config: " + order.getSymbol().code()));
        walletLedgerService.releaseOrderReserve(
                order.getUid(),
                config.getQuoteAsset(),
                reserved,
                order.getId().toString()
        );
        order.setReservedAmount(BigDecimal.ZERO);
    }
}
