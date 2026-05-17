/*
 * 檔案用途：領域服務，封裝撮合、風控、Polymarket 同步與交易規則。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.entity.PredictionPolymarketOrder;
import com.example.exchange.domain.repository.jpa.PredictionPolymarketOrderRepository;
import com.example.exchange.infra.config.PolymarketConfigs;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketOrderTrackingService {

    private static final List<String> RECONCILE_STATUSES =
            List.of("CREATED", "ACCEPTED", "live", "matched", "ORDER_STATUS_LIVE", "ORDER_STATUS_MATCHED");

    private final ObjectMapper objectMapper;
    private final PolymarketConfigs polymarketConfigs;
    private final PolymarketClobTradingClient clobTradingClient;
    private final PredictionPolymarketOrderRepository orderRepository;

    public List<PredictionPolymarketOrder> listLocalOrders() {
        return orderRepository.findAll();
    }

    public PredictionPolymarketOrder getLocalOrder(String internalOrderId) {
        return orderRepository.findByInternalOrderId(internalOrderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + internalOrderId));
    }

    @Transactional
    public PredictionPolymarketOrder syncOrder(String internalOrderId) {
        PredictionPolymarketOrder order =
                getLocalOrder(internalOrderId);

        if (order.getClobOrderId() == null || order.getClobOrderId().isBlank()) {
            order.setLastError("missing clobOrderId");
            return orderRepository.save(order);
        }

        Map<String, Object> raw =
                clobTradingClient.getOrder(
                        signerAddress(),
                        order.getClobOrderId()
                );

        applyClobOrderPayload(order, raw);

        return orderRepository.save(order);
    }

    @Transactional
    public PredictionPolymarketOrder cancelOrder(String internalOrderId) {
        PredictionPolymarketOrder order =
                getLocalOrder(internalOrderId);

        if (order.getClobOrderId() == null || order.getClobOrderId().isBlank()) {
            throw new IllegalStateException("missing clobOrderId");
        }

        Map<String, Object> raw =
                clobTradingClient.cancelOrder(
                        signerAddress(),
                        order.getClobOrderId()
                );

        order.setLastClobPayload(toJson(raw));
        order.setLastSyncedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(raw.get("success"))) {
            order.setStatus("CANCEL_REQUESTED");
            order.setLastError(null);
        } else {
            order.setLastError(firstText(raw, "errorMsg", "raw"));
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Map<String, Object> reconcileOpenOrders() {
        List<PredictionPolymarketOrder> orders =
                orderRepository.findByStatusInOrderByIdAsc(RECONCILE_STATUSES);

        int synced = 0;
        int failed = 0;

        for (PredictionPolymarketOrder order : orders) {
            try {
                if (order.getClobOrderId() == null || order.getClobOrderId().isBlank()) {
                    continue;
                }

                Map<String, Object> raw =
                        clobTradingClient.getOrder(
                                signerAddress(),
                                order.getClobOrderId()
                        );

                applyClobOrderPayload(order, raw);
                orderRepository.save(order);
                synced++;
            } catch (Exception e) {
                failed++;
                order.setLastError(e.getMessage());
                orderRepository.save(order);
            }
        }

        return Map.of(
                "total", orders.size(),
                "synced", synced,
                "failed", failed
        );
    }

    public Map<String, Object> getTrades() {
        return clobTradingClient.getTrades(signerAddress());
    }

    private void applyClobOrderPayload(
            PredictionPolymarketOrder order,
            Map<String, Object> raw
    ) {
        order.setLastClobPayload(toJson(raw));
        order.setLastSyncedAt(LocalDateTime.now());

        if (!Boolean.TRUE.equals(raw.get("success"))) {
            order.setLastError(firstText(raw, "errorMsg", "raw"));
            return;
        }

        String status =
                firstText(raw, "status");

        if (status != null) {
            order.setStatus(status);
        }

        BigDecimal sizeMatched =
                decimal(firstText(raw, "size_matched"));

        if (sizeMatched != null) {
            order.setSizeMatched(sizeMatched);
        }

        order.setLastError(null);
    }

    private String signerAddress() {
        String privateKey =
                polymarketConfigs.getWallet().getPrivateKey();

        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException("polymarket.wallet.private-key is required");
        }

        return Credentials.create(privateKey).getAddress();
    }

    private String firstText(
            Map<String, Object> raw,
            String... keys
    ) {
        for (String key : keys) {
            Object value =
                    raw.get(key);

            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }

        return null;
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
