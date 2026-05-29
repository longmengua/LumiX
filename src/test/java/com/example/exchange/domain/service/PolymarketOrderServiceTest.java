/*
 * 檔案用途：測試 Polymarket CLOB order idempotency baseline。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PolymarketPlaceOrderRequest;
import com.example.exchange.domain.model.dto.PolymarketPlaceOrderResponse;
import com.example.exchange.domain.model.entity.PredictionPolymarketOrder;
import com.example.exchange.domain.model.entity.PredictionSessionRecord;
import com.example.exchange.domain.model.enums.PolymarketClobSide;
import com.example.exchange.domain.model.enums.PolymarketOrderDirection;
import com.example.exchange.domain.model.enums.PolymarketOrderType;
import com.example.exchange.domain.repository.jpa.PredictionMarketInfoRepository;
import com.example.exchange.domain.repository.jpa.PredictionPolymarketOrderRepository;
import com.example.exchange.infra.config.PolymarketConfigs;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PolymarketOrderServiceTest {

    @Test
    @DisplayName("CLOB place 相同 clientRequestId 與 payload 重送會回傳既有成功結果")
    /**
     * 流程：client retry 同一個 clientRequestId，DB 已有同 payload 且已拿到 clobOrderId。
     * 期望：直接回 local record，不再 consume session limit、檢查 approval 或打 CLOB /order。
     */
    void duplicateClientRequestReturnsExistingAcceptedOrderWithoutRemoteCall() {
        PredictionPolymarketOrder existing = existingOrder("client-1", "ACCEPTED", "clob-1", "10.00");
        Fixture fx = new Fixture(existing);

        PolymarketPlaceOrderResponse response = fx.service.placeOrder(request("client-1", "10.000"));

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getInternalOrderId()).isEqualTo("client-1");
        assertThat(response.getClobOrderId()).isEqualTo("clob-1");
        assertThat(response.getSide()).isEqualTo(PolymarketClobSide.BUY);
        assertThat(response.getUsdtAmount()).isEqualByComparingTo("10.00");
        assertThat(fx.orderRepository.findCount).isEqualTo(1);
        assertThat(fx.sessionService.getActiveCount).isZero();
        assertThat(fx.sessionService.consumeLimitCount).isZero();
        assertThat(fx.approvalService.allowanceChecks).isZero();
        assertThat(fx.clobClient.postOrderCount).isZero();
        assertThat(fx.marketRepository.findCount).isZero();
    }

    @Test
    @DisplayName("CLOB place 相同 clientRequestId 但 payload 不同會拒絕")
    /**
     * 流程：clientRequestId 已用於 10 USDT，下次重送卻改成 11 USDT。
     * 期望：拒絕 idempotency conflict，避免同一 request identity 產生不同外部下單效果。
     */
    void duplicateClientRequestWithDifferentPayloadReturnsConflictWithoutRemoteCall() {
        PredictionPolymarketOrder existing = existingOrder("client-1", "ACCEPTED", "clob-1", "10.00");
        Fixture fx = new Fixture(existing);

        PolymarketPlaceOrderResponse response = fx.service.placeOrder(request("client-1", "11.00"));

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getStatus()).isEqualTo("IDEMPOTENCY_CONFLICT");
        assertThat(response.getErrorMsg()).contains("different payload");
        assertThat(fx.sessionService.getActiveCount).isZero();
        assertThat(fx.sessionService.consumeLimitCount).isZero();
        assertThat(fx.approvalService.allowanceChecks).isZero();
        assertThat(fx.clobClient.postOrderCount).isZero();
        assertThat(fx.marketRepository.findCount).isZero();
    }

    @Test
    @DisplayName("CLOB place 既有 local order 尚無終局結果時回報 uncertain")
    /**
     * 流程：第一次 request 已建立 local record，但尚未保存 CLOB terminal result。
     * 期望：duplicate 不再送第二次 /order，回報 outcome uncertain 交由查單/對帳處理。
     */
    void duplicateClientRequestWithCreatedLocalOrderReturnsUncertainWithoutRemoteCall() {
        PredictionPolymarketOrder existing = existingOrder("client-1", "CREATED", null, "10.00");
        Fixture fx = new Fixture(existing);

        PolymarketPlaceOrderResponse response = fx.service.placeOrder(request("client-1", "10.00"));

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getStatus()).isEqualTo("CLOB_OUTCOME_UNCERTAIN");
        assertThat(response.getErrorMsg()).contains("no terminal CLOB result");
        assertThat(fx.sessionService.getActiveCount).isZero();
        assertThat(fx.sessionService.consumeLimitCount).isZero();
        assertThat(fx.approvalService.allowanceChecks).isZero();
        assertThat(fx.clobClient.postOrderCount).isZero();
        assertThat(fx.marketRepository.findCount).isZero();
    }

    private static PolymarketPlaceOrderRequest request(String clientRequestId, String usdtAmount) {
        return PolymarketPlaceOrderRequest.builder()
                .userId("user-1")
                .clientRequestId(clientRequestId)
                .sessionId("session-1")
                .marketSlug("market-1")
                .direction(PolymarketOrderDirection.BUY_YES)
                .usdtAmount(new BigDecimal(usdtAmount))
                .orderType(PolymarketOrderType.FOK)
                .build();
    }

    private static PredictionPolymarketOrder existingOrder(
            String internalOrderId,
            String status,
            String clobOrderId,
            String usdtAmount
    ) {
        PredictionPolymarketOrder order = new PredictionPolymarketOrder();
        order.setInternalOrderId(internalOrderId);
        order.setClobOrderId(clobOrderId);
        order.setUserId("user-1");
        order.setSessionId("session-1");
        order.setMarketSlug("market-1");
        order.setDirection(PolymarketOrderDirection.BUY_YES.name());
        order.setSide(PolymarketClobSide.BUY.name());
        order.setOrderType(PolymarketOrderType.FOK.name());
        order.setTokenId("token-yes");
        order.setPrice(new BigDecimal("0.50"));
        order.setSize(new BigDecimal("20.000000"));
        order.setUsdtAmount(new BigDecimal(usdtAmount));
        order.setStatus(status);
        return order;
    }

    private static class Fixture {
        private final PolymarketConfigs configs = configs();
        private final CountingClobClient clobClient = new CountingClobClient(configs);
        private final CountingSessionService sessionService = new CountingSessionService();
        private final CountingApprovalService approvalService = new CountingApprovalService(configs);
        private final CountingMarketRepository marketRepository = new CountingMarketRepository();
        private final CountingOrderRepository orderRepository;
        private final PolymarketOrderService service;

        private Fixture(PredictionPolymarketOrder existing) {
            orderRepository = new CountingOrderRepository(existing);
            service = new PolymarketOrderService(
                configs,
                clobClient,
                sessionService,
                approvalService,
                marketRepository.proxy(),
                orderRepository.proxy()
            );
        }

        private static PolymarketConfigs configs() {
            PolymarketConfigs configs = new PolymarketConfigs();
            configs.getClob().setBaseUrl("https://clob.example");
            configs.getClob().setApiKey("api-key");
            configs.getClob().setApiSecret("api-secret");
            configs.getClob().setApiPassphrase("api-passphrase");
            configs.getWallet().setFunderAddress("0x0000000000000000000000000000000000000001");
            configs.getWallet().setPrivateKey("0x0123456789012345678901234567890123456789012345678901234567890123");
            return configs;
        }
    }

    private static class CountingClobClient extends PolymarketClobTradingClient {
        private int postOrderCount;

        private CountingClobClient(PolymarketConfigs configs) {
            super(new ObjectMapper(), configs, null);
        }

        @Override
        public PolymarketPlaceOrderResponse postOrder(String polygonSignerAddress, com.example.exchange.domain.model.dto.PolymarketClobOrderRequest clobOrderRequest) {
            postOrderCount++;
            return PolymarketPlaceOrderResponse.builder()
                    .success(true)
                    .clobOrderId("clob-new")
                    .status("ACCEPTED")
                    .build();
        }
    }

    private static class CountingSessionService extends PolymarketSessionService {
        private int getActiveCount;
        private int consumeLimitCount;

        private CountingSessionService() {
            super(null);
        }

        @Override
        public PredictionSessionRecord getActiveSession(String sessionId) {
            getActiveCount++;
            return PredictionSessionRecord.builder()
                    .sessionId(sessionId)
                    .sessionPrivateKey("0x0123456789012345678901234567890123456789012345678901234567890123")
                    .build();
        }

        @Override
        public void assertAndConsumeLimit(PredictionSessionRecord session, BigDecimal usdtAmount) {
            consumeLimitCount++;
        }
    }

    private static class CountingApprovalService extends PolymarketApprovalService {
        private int allowanceChecks;

        private CountingApprovalService(PolymarketConfigs configs) {
            super(null, configs);
        }

        @Override
        public void requireCollateralAllowance(String owner, BigInteger requiredAmount) {
            allowanceChecks++;
        }

        @Override
        public void requireConditionalTokensApproval(String owner) {
            allowanceChecks++;
        }
    }

    private static class CountingMarketRepository {
        private int findCount;

        private PredictionMarketInfoRepository proxy() {
            return (PredictionMarketInfoRepository) Proxy.newProxyInstance(
                    PredictionMarketInfoRepository.class.getClassLoader(),
                    new Class<?>[]{PredictionMarketInfoRepository.class},
                    (proxy, method, args) -> {
                        if ("findByMarketSlug".equals(method.getName())) {
                            findCount++;
                            return Optional.empty();
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static class CountingOrderRepository {
        private final PredictionPolymarketOrder existing;
        private int findCount;

        private CountingOrderRepository(PredictionPolymarketOrder existing) {
            this.existing = existing;
        }

        private PredictionPolymarketOrderRepository proxy() {
            return (PredictionPolymarketOrderRepository) Proxy.newProxyInstance(
                    PredictionPolymarketOrderRepository.class.getClassLoader(),
                    new Class<?>[]{PredictionPolymarketOrderRepository.class},
                    (proxy, method, args) -> {
                        if ("findByInternalOrderId".equals(method.getName())) {
                            findCount++;
                            return Optional.ofNullable(existing);
                        }
                        if ("save".equals(method.getName())) {
                            return args[0];
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
