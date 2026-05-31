/*
 * 檔案用途：測試做市商 quote order gateway 只撤掉自身 stale quote orders。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.usecase.CancelOrderUseCase;
import com.example.exchange.domain.model.dto.MarketMakerQuoteCommand;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UseCaseMarketMakerQuoteOrderGatewayTest {

    @Test
    @DisplayName("cancelOpenQuoteOrders 只撤同 uid/symbol 且 clientOrderId 屬於該做市商的 quote orders")
    void cancelOpenQuoteOrdersFiltersByMarketMakerQuotePrefix() {
        MemOrderRepository orderRepository = new MemOrderRepository();
        RecordingCancelOrderUseCase cancelOrderUseCase = new RecordingCancelOrderUseCase();
        UseCaseMarketMakerQuoteOrderGateway gateway = new UseCaseMarketMakerQuoteOrderGateway(
                null,
                cancelOrderUseCase,
                orderRepository
        );
        Order staleBid = order("mmq:mm-1:old:buy");
        Order staleAsk = order("mmq:mm-1:old:sell");
        Order manualOrder = order("manual-1");
        Order otherMarketMaker = order("mmq:mm-2:old:buy");
        orderRepository.orders.addAll(List.of(staleBid, staleAsk, manualOrder, otherMarketMaker));

        // 場景：做市商送新 quote 前只能清理自己的舊 quote，不能影響同帳戶其他策略或其他做市商前綴。
        int canceled = gateway.cancelOpenQuoteOrders(quote());

        assertThat(canceled).isEqualTo(2);
        assertThat(cancelOrderUseCase.canceledOrderIds).containsExactly(staleBid.getId(), staleAsk.getId());
    }

    private static MarketMakerQuoteCommand quote() {
        return new MarketMakerQuoteCommand(
                "mm-1",
                9101,
                "BTCUSDT",
                new BigDecimal("100.00"),
                new BigDecimal("1.000"),
                new BigDecimal("102.00"),
                new BigDecimal("1.000"),
                "new"
        );
    }

    private static Order order(String clientOrderId) {
        return Order.builder()
                .id(UUID.randomUUID())
                .uid(9101)
                .symbol(Symbol.builder().base("BTC").quote("USDT").build())
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("100.00"))
                .qty(new BigDecimal("1.000"))
                .origQty(new BigDecimal("1.000"))
                .clientOrderId(clientOrderId)
                .status(Order.Status.NEW)
                .build();
    }

    private static final class RecordingCancelOrderUseCase extends CancelOrderUseCase {
        private final List<UUID> canceledOrderIds = new ArrayList<>();

        private RecordingCancelOrderUseCase() {
            super(null, null, null, null, null, null);
        }

        @Override
        public boolean handle(UUID orderId) {
            canceledOrderIds.add(orderId);
            return true;
        }
    }

    private static final class MemOrderRepository implements OrderRepository {
        private final List<Order> orders = new ArrayList<>();

        @Override
        public Optional<Order> findById(UUID id) {
            return orders.stream()
                    .filter(order -> order.getId().equals(id))
                    .findFirst();
        }

        @Override
        public void save(Order order) {
        }

        @Override
        public List<Order> openOrders(long uid) {
            return orders.stream()
                    .filter(order -> order.getUid() == uid)
                    .filter(order -> order.getStatus() == Order.Status.NEW || order.getStatus() == Order.Status.PARTIALLY_FILLED)
                    .toList();
        }

        @Override
        public List<Order> findOpenOrders(Long uid, String symbol) {
            return openOrders(uid).stream()
                    .filter(order -> order.getSymbol().code().equals(symbol))
                    .toList();
        }

        @Override
        public List<Order> findAllOrders(Long uid, String symbol) {
            return orders.stream()
                    .filter(order -> order.getUid() == uid)
                    .filter(order -> order.getSymbol().code().equals(symbol))
                    .toList();
        }
    }
}
