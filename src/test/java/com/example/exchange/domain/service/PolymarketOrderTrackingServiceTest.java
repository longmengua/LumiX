/*
 * 檔案用途：測試 Polymarket order tracking 的 cancel idempotency baseline。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.entity.PredictionPolymarketOrder;
import com.example.exchange.domain.repository.jpa.PredictionPolymarketOrderRepository;
import com.example.exchange.infra.config.PolymarketConfigs;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PolymarketOrderTrackingServiceTest {

    @Test
    @DisplayName("CLOB cancel 重送遇到本地已取消中狀態時不再送 DELETE")
    /**
     * 流程：前一次 cancel 已成功把 local order 標成 CANCEL_REQUESTED，client 或 job retry。
     * 期望：直接回傳本地 order，避免對 CLOB /order 重送第二次 DELETE。
     */
    void cancelRetryWithCancelRequestedStatusReturnsLocalOrderWithoutRemoteCall() {
        PredictionPolymarketOrder existing = order("CANCEL_REQUESTED", "clob-1");
        Fixture fx = new Fixture(existing);

        PredictionPolymarketOrder result = fx.service.cancelOrder("internal-1");

        assertThat(result).isSameAs(existing);
        assertThat(result.getStatus()).isEqualTo("CANCEL_REQUESTED");
        assertThat(fx.clobClient.cancelCount).isZero();
        assertThat(fx.orderRepository.saveCount).isZero();
    }

    @Test
    @DisplayName("CLOB cancel 第一次成功時記錄 cancel requested 供後續 retry 回放")
    /**
     * 流程：local order 尚未取消，CLOB DELETE 回 success。
     * 期望：保存 CLOB payload、同步時間與 CANCEL_REQUESTED，形成後續重送的本地 idempotency boundary。
     */
    void successfulCancelRecordsCancelRequestedStatus() {
        PredictionPolymarketOrder existing = order("ACCEPTED", "clob-1");
        Fixture fx = new Fixture(existing);

        PredictionPolymarketOrder result = fx.service.cancelOrder("internal-1");

        assertThat(result.getStatus()).isEqualTo("CANCEL_REQUESTED");
        assertThat(result.getLastError()).isNull();
        assertThat(result.getLastClobPayload()).contains("\"success\":true");
        assertThat(result.getLastSyncedAt()).isNotNull();
        assertThat(fx.clobClient.cancelCount).isEqualTo(1);
        assertThat(fx.orderRepository.saveCount).isEqualTo(1);
    }

    private static PredictionPolymarketOrder order(String status, String clobOrderId) {
        PredictionPolymarketOrder order = new PredictionPolymarketOrder();
        order.setInternalOrderId("internal-1");
        order.setClobOrderId(clobOrderId);
        order.setUserId("user-1");
        order.setSessionId("session-1");
        order.setMarketSlug("market-1");
        order.setStatus(status);
        return order;
    }

    private static class Fixture {
        private final PolymarketConfigs configs = configs();
        private final CountingClobClient clobClient = new CountingClobClient(configs);
        private final CountingOrderRepository orderRepository;
        private final PolymarketOrderTrackingService service;

        private Fixture(PredictionPolymarketOrder existing) {
            orderRepository = new CountingOrderRepository(existing);
            service = new PolymarketOrderTrackingService(
                    new ObjectMapper(),
                    configs,
                    clobClient,
                    orderRepository.proxy()
            );
        }

        private static PolymarketConfigs configs() {
            PolymarketConfigs configs = new PolymarketConfigs();
            configs.getWallet().setPrivateKey("0x0123456789012345678901234567890123456789012345678901234567890123");
            return configs;
        }
    }

    private static class CountingClobClient extends PolymarketClobTradingClient {
        private int cancelCount;

        private CountingClobClient(PolymarketConfigs configs) {
            super(new ObjectMapper(), configs, null);
        }

        @Override
        public Map<String, Object> cancelOrder(String polygonSignerAddress, String clobOrderId) {
            cancelCount++;
            return Map.of(
                    "success", true,
                    "orderID", clobOrderId
            );
        }
    }

    private static class CountingOrderRepository {
        private final PredictionPolymarketOrder existing;
        private int saveCount;

        private CountingOrderRepository(PredictionPolymarketOrder existing) {
            this.existing = existing;
        }

        private PredictionPolymarketOrderRepository proxy() {
            return (PredictionPolymarketOrderRepository) Proxy.newProxyInstance(
                    PredictionPolymarketOrderRepository.class.getClassLoader(),
                    new Class<?>[]{PredictionPolymarketOrderRepository.class},
                    (proxy, method, args) -> {
                        if ("findByInternalOrderId".equals(method.getName())) {
                            return Optional.of(existing);
                        }
                        if ("save".equals(method.getName())) {
                            saveCount++;
                            return args[0];
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
