/*
 * 檔案用途：UseCase 入口，承接介面層請求並協調應用服務執行業務動作。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.command.CancelReplaceOrderCommand;
import com.example.exchange.application.command.PlaceOrderCommand;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.interfaces.web.dto.OrderInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class CancelReplaceOrderUseCase {

    private final OrderRepository orderRepository;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final PlaceOrderUseCase placeOrderUseCase;

    public OrderInfoResponse handle(CancelReplaceOrderCommand command) {
        validateCommand(command);

        Order original = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + command.orderId()));
        if (original.getUid() != command.uid()) {
            throw new IllegalArgumentException("order uid mismatch");
        }
        if (!isOpen(original)) {
            throw new IllegalStateException("only open orders can be cancel-replaced");
        }

        BigDecimal replacementPrice = command.price() == null ? original.getPrice() : command.price();
        BigDecimal replacementQty = command.qty() == null ? original.getQty() : command.qty();
        String clientOrderId = command.clientOrderId() == null ? original.getClientOrderId() : command.clientOrderId();

        boolean canceled = cancelOrderUseCase.handle(original.getId());
        if (!canceled) {
            throw new IllegalStateException("cancel-replace failed to cancel original order");
        }

        Order replacement = placeOrderUseCase.place(new PlaceOrderCommand(
                original.getUid(),
                original.getSymbol().code(),
                original.getSide(),
                original.getType(),
                replacementPrice,
                replacementQty,
                original.getLeverage(),
                original.getMarginMode().name(),
                clientOrderId,
                original.getTimeInForce().name(),
                original.isReduceOnly(),
                original.isPostOnly()
        ));
        return replacement.toOrderInfoResponse();
    }

    private void validateCommand(CancelReplaceOrderCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("cancel-replace command cannot be null");
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

    private static boolean isOpen(Order order) {
        return order.getStatus() == Order.Status.NEW
                || order.getStatus() == Order.Status.PARTIALLY_FILLED;
    }
}
