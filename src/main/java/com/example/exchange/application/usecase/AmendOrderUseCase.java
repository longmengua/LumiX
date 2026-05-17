/*
 * 檔案用途：UseCase 入口，承接介面層請求並協調應用服務執行業務動作。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.command.AmendOrderCommand;
import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.application.service.MarketDataService;
import com.example.exchange.application.service.RiskService;
import com.example.exchange.domain.event.OrderLifecycleEvent;
import com.example.exchange.domain.model.dto.TopOfBook;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.domain.service.MatchingEngine;
import com.example.exchange.domain.service.OrderBookSnapshot;
import com.example.exchange.interfaces.web.dto.OrderInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AmendOrderUseCase {

    private final OrderRepository orderRepository;
    private final SymbolConfigRepository symbolConfigRepository;
    private final MatchingEngine matchingEngine;
    private final RiskService riskService;
    private final MarketDataService marketDataService;
    private final DomainEventPublisher<Object> publisher;

    public OrderInfoResponse handle(AmendOrderCommand command) {
        validateCommand(command);

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + command.orderId()));
        if (order.getUid() != command.uid()) {
            throw new IllegalArgumentException("order uid mismatch");
        }
        if (!isOpen(order)) {
            throw new IllegalStateException("only open orders can be amended");
        }
        if (order.getType() != OrderType.LIMIT) {
            throw new IllegalArgumentException("only LIMIT orders can be amended");
        }

        BigDecimal newPrice = command.price() == null ? order.getPrice() : command.price();
        BigDecimal newQty = command.qty() == null ? order.getQty() : command.qty();
        String newClientOrderId = command.clientOrderId() == null
                ? order.getClientOrderId()
                : command.clientOrderId();

        SymbolConfig config = symbolConfigRepository.findBySymbol(order.getSymbol().code())
                .orElseThrow(() -> new IllegalArgumentException("missing symbol config: " + order.getSymbol().code()));
        Order candidate = amendedCandidate(order, newPrice, newQty, newClientOrderId);
        if (wouldTakeLiquidity(candidate)) {
            throw new IllegalArgumentException("amend would take liquidity; use cancel-replace instead");
        }

        BigDecimal targetReserve = riskService.validateAmend(candidate, config);
        boolean amended = matchingEngine.amendOrder(order, newPrice, newQty);
        if (!amended) {
            throw new IllegalStateException("open order not found in matching book");
        }

        order.setOrigQty(order.getExecutedQty().add(newQty));
        order.setClientOrderId(newClientOrderId);
        order.setStatus(order.getExecutedQty().signum() > 0 ? Order.Status.PARTIALLY_FILLED : Order.Status.NEW);
        riskService.reconcileOrderReserve(order, config, targetReserve);
        orderRepository.save(order);
        publisher.publish(OrderLifecycleEvent.updated(order));

        OrderBookSnapshot snapshot = matchingEngine.snapshot(order.getSymbol().code(), 50);
        marketDataService.onOrderBookChanged(
                order.getSymbol().code(),
                snapshot,
                matchingEngine.top(order.getSymbol().code())
        );
        return order.toOrderInfoResponse();
    }

    private void validateCommand(AmendOrderCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("amend order command cannot be null");
        }
        if (command.orderId() == null) {
            throw new IllegalArgumentException("orderId cannot be null");
        }
        if (command.price() == null && command.qty() == null && command.clientOrderId() == null) {
            throw new IllegalArgumentException("price, qty or clientOrderId is required");
        }
        if (command.price() != null && command.price().signum() <= 0) {
            throw new IllegalArgumentException("price must be greater than zero");
        }
        if (command.qty() != null && command.qty().signum() <= 0) {
            throw new IllegalArgumentException("qty must be greater than zero");
        }
    }

    private boolean wouldTakeLiquidity(Order candidate) {
        Optional<TopOfBook> top = matchingEngine.top(candidate.getSymbol().code());
        if (top.isEmpty()) return false;
        BigDecimal opposite = candidate.getSide() == OrderSide.BUY
                ? top.get().getBestAsk()
                : top.get().getBestBid();
        if (opposite == null || opposite.signum() <= 0) return false;
        return candidate.getSide() == OrderSide.BUY
                ? candidate.getPrice().compareTo(opposite) >= 0
                : candidate.getPrice().compareTo(opposite) <= 0;
    }

    private static Order amendedCandidate(
            Order order,
            BigDecimal newPrice,
            BigDecimal newQty,
            String newClientOrderId
    ) {
        return Order.builder()
                .id(order.getId())
                .uid(order.getUid())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .type(order.getType())
                .price(newPrice)
                .qty(newQty)
                .origQty(order.getExecutedQty().add(newQty))
                .executedQty(order.getExecutedQty())
                .avgPrice(order.getAvgPrice())
                .timeInForce(order.getTimeInForce())
                .reduceOnly(order.isReduceOnly())
                .postOnly(order.isPostOnly())
                .leverage(order.getLeverage())
                .marginMode(order.getMarginMode())
                .reservedAmount(order.getReservedAmount())
                .clientOrderId(newClientOrderId)
                .status(order.getExecutedQty().signum() > 0 ? Order.Status.PARTIALLY_FILLED : Order.Status.NEW)
                .ctime(order.getCtime())
                .build();
    }

    private static boolean isOpen(Order order) {
        return order.getStatus() == Order.Status.NEW
                || order.getStatus() == Order.Status.PARTIALLY_FILLED;
    }
}
