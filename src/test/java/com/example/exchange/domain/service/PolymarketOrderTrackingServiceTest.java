/*
 * 檔案用途：測試 Polymarket order tracking 的 cancel idempotency baseline。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.entity.PredictionPolymarketOrder;
import com.example.exchange.domain.model.dto.PolymarketClobCommandRecord;
import com.example.exchange.domain.repository.PolymarketClobCommandStore;
import com.example.exchange.domain.repository.jpa.PredictionPolymarketOrderRepository;
import com.example.exchange.infra.config.PolymarketConfigs;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

    @Test
    @DisplayName("CLOB cancel exception outcome 會標記 uncertain 並阻止後續重送")
    /**
     * 流程：CLOB DELETE 發生 timeout/exception，遠端可能已收到 cancel。
     * 期望：local order 標成 CANCEL_OUTCOME_UNCERTAIN；下一次 retry 不再送第二次 DELETE。
     */
    void cancelExceptionOutcomeIsMarkedUncertainAndReplayBlocksRemoteRetry() {
        PredictionPolymarketOrder existing = order("ACCEPTED", "clob-1");
        Fixture fx = new Fixture(existing);
        fx.clobClient.nextCancelOrder = Map.of(
                "success", false,
                "status", "EXCEPTION",
                "errorMsg", "network timeout"
        );

        PredictionPolymarketOrder first = fx.service.cancelOrder("internal-1");
        PredictionPolymarketOrder duplicate = fx.service.cancelOrder("internal-1");

        assertThat(first.getStatus()).isEqualTo("CANCEL_OUTCOME_UNCERTAIN");
        assertThat(first.getLastError()).isEqualTo("network timeout");
        assertThat(first.getLastClobPayload()).contains("\"status\":\"EXCEPTION\"");
        assertThat(duplicate).isSameAs(existing);
        assertThat(fx.clobClient.cancelCount).isEqualTo(1);
        assertThat(fx.orderRepository.saveCount).isEqualTo(1);
    }

    @Test
    @DisplayName("CLOB cancel 5xx outcome 會標記 uncertain")
    /**
     * 流程：CLOB DELETE 回 5xx，無法判定遠端是否已處理 cancel。
     * 期望：保存 uncertain 狀態，避免 retry path 直接產生第二次外部 effect。
     */
    void cancelServerErrorOutcomeIsMarkedUncertain() {
        PredictionPolymarketOrder existing = order("ACCEPTED", "clob-1");
        Fixture fx = new Fixture(existing);
        fx.clobClient.nextCancelOrder = Map.of(
                "success", false,
                "httpCode", 502,
                "errorMsg", "bad gateway"
        );

        PredictionPolymarketOrder result = fx.service.cancelOrder("internal-1");

        assertThat(result.getStatus()).isEqualTo("CANCEL_OUTCOME_UNCERTAIN");
        assertThat(result.getLastError()).isEqualTo("bad gateway");
        assertThat(fx.clobClient.cancelCount).isEqualTo(1);
        assertThat(fx.orderRepository.saveCount).isEqualTo(1);
    }

    @Test
    @DisplayName("CLOB cancel 相同 commandId 重送會用 durable command record 擋下")
    /**
     * 流程：第一次 cancel 帶 commandId 並成功，client retry 同一 commandId。
     * 期望：第二次直接回 local order，不再送 CLOB DELETE；command record 保存 terminal result。
     */
    void duplicateCancelCommandIdReturnsLocalOrderWithoutRemoteRetry() {
        PredictionPolymarketOrder existing = order("ACCEPTED", "clob-1");
        Fixture fx = new Fixture(existing);

        PredictionPolymarketOrder first = fx.service.cancelOrder("internal-1", "cancel-command-1");
        PredictionPolymarketOrder duplicate = fx.service.cancelOrder("internal-1", "cancel-command-1");

        assertThat(first.getStatus()).isEqualTo("CANCEL_REQUESTED");
        assertThat(duplicate).isSameAs(existing);
        assertThat(fx.clobClient.cancelCount).isEqualTo(1);
        assertThat(fx.commandStore.records.get("cancel-command-1").completed()).isTrue();
        assertThat(fx.commandStore.records.get("cancel-command-1").resultStatus()).isEqualTo("CANCEL_REQUESTED");
    }

    @Test
    @DisplayName("CLOB cancel commandId payload conflict 會拒絕且不呼叫 CLOB")
    /**
     * 流程：commandId 已 claim 給另一個 internal order，這次拿同一 commandId 取消目前 order。
     * 期望：拒絕 command identity conflict，避免同一外部 command identity 指向不同 CLOB effect。
     */
    void cancelCommandIdConflictIsRejectedBeforeRemoteCall() {
        PredictionPolymarketOrder existing = order("ACCEPTED", "clob-1");
        Fixture fx = new Fixture(existing);
        fx.commandStore.claim("cancel-command-1", "CANCEL", "other-order", "CANCEL|other-order|clob-2");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> fx.service.cancelOrder("internal-1", "cancel-command-1")
                )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commandId");
        assertThat(fx.clobClient.cancelCount).isZero();
        assertThat(fx.orderRepository.saveCount).isZero();
    }



    @Test
    @DisplayName("CLOB sync 重送相同 payload 時不重複保存 local order")
    /**
     * 流程：local order 已套用過同一份 CLOB getOrder payload，retry sync 再取回相同內容。
     * 期望：仍可安全查 CLOB read-only API，但不重複 save local row，避免 retry 製造新的本地狀態變更。
     */
    void syncRetryWithSamePayloadDoesNotSaveLocalOrderAgain() {
        PredictionPolymarketOrder existing = order("live", "clob-1");
        existing.setSizeMatched(new BigDecimal("2.0"));
        existing.setLastError(null);
        existing.setLastClobPayload("{\"success\":true,\"status\":\"live\",\"size_matched\":\"2.00\"}");
        LocalDateTime lastSyncedAt = LocalDateTime.now().minusMinutes(1);
        existing.setLastSyncedAt(lastSyncedAt);
        Fixture fx = new Fixture(existing);
        fx.clobClient.nextGetOrder = successOrderPayload("live", "2.00");

        PredictionPolymarketOrder result = fx.service.syncOrder("internal-1");

        assertThat(result).isSameAs(existing);
        assertThat(result.getLastSyncedAt()).isEqualTo(lastSyncedAt);
        assertThat(fx.clobClient.getOrderCount).isEqualTo(1);
        assertThat(fx.orderRepository.saveCount).isZero();
    }

    @Test
    @DisplayName("CLOB reconcile 對未變更 payload 計入 unchanged 且不保存")
    /**
     * 流程：reconcile open orders 取回的 CLOB 狀態與 local row 完全一致。
     * 期望：報表標記 checked-but-unchanged，且不 save local row，讓 reconcile retry 不製造重複本地效果。
     */
    void reconcileUnchangedPayloadReportsUnchangedWithoutSave() {
        PredictionPolymarketOrder existing = order("live", "clob-1");
        existing.setSizeMatched(new BigDecimal("2.0"));
        existing.setLastClobPayload("{\"success\":true,\"status\":\"live\",\"size_matched\":\"2.00\"}");
        Fixture fx = new Fixture(existing);
        fx.clobClient.nextGetOrder = successOrderPayload("live", "2.00");

        Map<String, Object> result = fx.service.reconcileOpenOrders();

        assertThat(result).containsEntry("total", 1);
        assertThat(result).containsEntry("synced", 1);
        assertThat(result).containsEntry("unchanged", 1);
        assertThat(result).containsEntry("failed", 0);
        assertThat(fx.orderRepository.saveCount).isZero();
    }

    @Test
    @DisplayName("CLOB reconcile 會納入 cancel uncertain order 並用遠端狀態解除")
    /**
     * 流程：cancel timeout 後 local order 是 CANCEL_OUTCOME_UNCERTAIN，reconcile 查 CLOB getOrder。
     * 期望：reconcile query 納入 uncertain 狀態，遠端若已取消就更新 local status，解除不確定狀態。
     */
    void reconcileIncludesCancelUncertainOrdersAndResolvesRemoteCanceledStatus() {
        PredictionPolymarketOrder existing = order("CANCEL_OUTCOME_UNCERTAIN", "clob-1");
        existing.setLastError("network timeout");
        Fixture fx = new Fixture(existing);
        fx.clobClient.nextGetOrder = successOrderPayload("ORDER_STATUS_CANCELED", "0");

        Map<String, Object> result = fx.service.reconcileOpenOrders();

        assertThat(result).containsEntry("total", 1);
        assertThat(result).containsEntry("synced", 1);
        assertThat(result).containsEntry("unchanged", 0);
        assertThat(result).containsEntry("failed", 0);
        assertThat(existing.getStatus()).isEqualTo("ORDER_STATUS_CANCELED");
        assertThat(existing.getLastError()).isNull();
        assertThat(fx.orderRepository.lastStatusQuery).contains("CANCEL_OUTCOME_UNCERTAIN");
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

    private static Map<String, Object> successOrderPayload(String status, String sizeMatched) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("status", status);
        payload.put("size_matched", sizeMatched);
        return payload;
    }

    private static class Fixture {
        private final PolymarketConfigs configs = configs();
        private final CountingClobClient clobClient = new CountingClobClient(configs);
        private final CountingOrderRepository orderRepository;
        private final FakeClobCommandStore commandStore = new FakeClobCommandStore();
        private final PolymarketOrderTrackingService service;

        private Fixture(PredictionPolymarketOrder existing) {
            orderRepository = new CountingOrderRepository(existing);
            service = new PolymarketOrderTrackingService(
                    new ObjectMapper(),
                    configs,
                    clobClient,
                    orderRepository.proxy(),
                    commandStore
            );
        }

        private static PolymarketConfigs configs() {
            PolymarketConfigs configs = new PolymarketConfigs();
            configs.getWallet().setPrivateKey("0x0123456789012345678901234567890123456789012345678901234567890123");
            return configs;
        }
    }

    private static class FakeClobCommandStore implements PolymarketClobCommandStore {
        private final Map<String, PolymarketClobCommandRecord> records =
                new ConcurrentHashMap<>();

        @Override
        public Optional<PolymarketClobCommandRecord> find(String commandId) {
            return Optional.ofNullable(records.get(commandId));
        }

        @Override
        public boolean claim(String commandId, String commandType, String internalOrderId, String fingerprint) {
            PolymarketClobCommandRecord record =
                    new PolymarketClobCommandRecord(
                            commandId,
                            commandType,
                            internalOrderId,
                            fingerprint,
                            false,
                            null,
                            null
                    );
            return records.putIfAbsent(commandId, record) == null;
        }

        @Override
        public PolymarketClobCommandRecord complete(String commandId, String resultStatus, String lastError) {
            return records.compute(commandId, (key, existing) -> new PolymarketClobCommandRecord(
                    existing.commandId(),
                    existing.commandType(),
                    existing.internalOrderId(),
                    existing.fingerprint(),
                    true,
                    resultStatus,
                    lastError
            ));
        }
    }

    private static class CountingClobClient extends PolymarketClobTradingClient {
        private int cancelCount;
        private int getOrderCount;
        private Map<String, Object> nextCancelOrder =
                Map.of(
                        "success", true,
                        "orderID", "clob-1"
                );
        private Map<String, Object> nextGetOrder =
                successOrderPayload("live", "0");

        private CountingClobClient(PolymarketConfigs configs) {
            super(new ObjectMapper(), configs, null);
        }

        @Override
        public Map<String, Object> cancelOrder(String polygonSignerAddress, String clobOrderId) {
            cancelCount++;
            return nextCancelOrder;
        }

        @Override
        public Map<String, Object> getOrder(String polygonSignerAddress, String clobOrderId) {
            getOrderCount++;
            return nextGetOrder;
        }
    }

    private static class CountingOrderRepository {
        private final PredictionPolymarketOrder existing;
        private int saveCount;
        private List<String> lastStatusQuery = List.of();

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
                        if ("findByStatusInOrderByIdAsc".equals(method.getName())) {
                            lastStatusQuery = (List<String>) args[0];
                            return List.of(existing);
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
