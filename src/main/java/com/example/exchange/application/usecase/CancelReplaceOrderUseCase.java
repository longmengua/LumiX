/*
 * 檔案用途：UseCase 入口，承接介面層請求並協調應用服務執行業務動作。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.command.CancelReplaceOrderCommand;
import com.example.exchange.application.command.PlaceOrderCommand;
import com.example.exchange.application.service.CommandTransactionBoundary;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.interfaces.web.dto.OrderInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class CancelReplaceOrderUseCase {

    private final OrderRepository orderRepository;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final PlaceOrderUseCase placeOrderUseCase;
    private CommandTransactionBoundary commandTransactionBoundary;

    @Autowired(required = false)
    public void setCommandTransactionBoundary(CommandTransactionBoundary commandTransactionBoundary) {
        this.commandTransactionBoundary = commandTransactionBoundary;
    }

    /**
     * 先撤原單，再用原單參數加上 request override 建立新單。
     *
     * <p>Spring runtime 會用 command transaction boundary 包住撤原單與 replacement 下單，
     * 避免資料庫狀態出現「撤單已提交但 replacement 失敗」的半套結果。</p>
     */
    public OrderInfoResponse handle(CancelReplaceOrderCommand command) {
        if (commandTransactionBoundary != null) {
            return commandTransactionBoundary.execute("cancel-replace-order", () -> handleInsideTransaction(command));
        }
        return handleInsideTransaction(command);
    }

    private OrderInfoResponse handleInsideTransaction(CancelReplaceOrderCommand command) {
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

        // Cancel-replace 的第一步一定要撤掉原單並釋放 reserve，避免 replacement 重複占用資金。
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

    /** 驗證 cancel-replace 至少覆寫 price、qty 或 clientOrderId 其中一項。 */
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
