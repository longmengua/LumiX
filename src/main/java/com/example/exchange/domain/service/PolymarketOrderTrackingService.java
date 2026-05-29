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
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketOrderTrackingService {

    private static final List<String> RECONCILE_STATUSES =
            List.of(
                    "CREATED",
                    "ACCEPTED",
                    "CANCEL_OUTCOME_UNCERTAIN",
                    "live",
                    "matched",
                    "ORDER_STATUS_LIVE",
                    "ORDER_STATUS_MATCHED"
            );

    private static final List<String> CANCEL_IDEMPOTENT_STATUSES =
            List.of("CANCEL_REQUESTED", "CANCEL_OUTCOME_UNCERTAIN", "CANCELED", "CANCELLED", "ORDER_STATUS_CANCELED");

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

        boolean changed =
                applyClobOrderPayloadIfChanged(order, raw);

        if (!changed) {
            return order;
        }

        return orderRepository.save(order);
    }

    @Transactional
    public PredictionPolymarketOrder cancelOrder(String internalOrderId) {
        PredictionPolymarketOrder order =
                getLocalOrder(internalOrderId);

        if (isCancelAlreadyRecorded(order)) {
            return order;
        }

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
        } else if (isUncertainCancelOutcome(raw)) {
            order.setStatus("CANCEL_OUTCOME_UNCERTAIN");
            order.setLastError(firstText(raw, "errorMsg", "raw", "status"));
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
        int unchanged = 0;
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

                boolean changed =
                        applyClobOrderPayloadIfChanged(order, raw);
                if (changed) {
                    orderRepository.save(order);
                } else {
                    unchanged++;
                }
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
                "unchanged", unchanged,
                "failed", failed
        );
    }

    public Map<String, Object> getTrades() {
        return clobTradingClient.getTrades(signerAddress());
    }

    private boolean applyClobOrderPayloadIfChanged(
            PredictionPolymarketOrder order,
            Map<String, Object> raw
    ) {
        String nextPayload =
                toJson(raw);
        String nextStatus =
                order.getStatus();
        BigDecimal nextSizeMatched =
                order.getSizeMatched();
        String nextLastError;

        if (!Boolean.TRUE.equals(raw.get("success"))) {
            nextLastError =
                    firstText(raw, "errorMsg", "raw");
        } else {
            String status =
                    firstText(raw, "status");

            if (status != null) {
                nextStatus =
                        status;
            }

            BigDecimal sizeMatched =
                    decimal(firstText(raw, "size_matched"));

            if (sizeMatched != null) {
                nextSizeMatched =
                        sizeMatched;
            }

            nextLastError =
                    null;
        }

        if (Objects.equals(order.getLastClobPayload(), nextPayload)
                && Objects.equals(order.getStatus(), nextStatus)
                && compareDecimal(order.getSizeMatched(), nextSizeMatched)
                && Objects.equals(order.getLastError(), nextLastError)) {
            return false;
        }

        order.setLastClobPayload(nextPayload);
        order.setLastSyncedAt(LocalDateTime.now());
        order.setStatus(nextStatus);
        order.setSizeMatched(nextSizeMatched);
        order.setLastError(nextLastError);
        return true;
    }

    private String signerAddress() {
        String privateKey =
                polymarketConfigs.getWallet().getPrivateKey();

        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException("polymarket.wallet.private-key is required");
        }

        return Credentials.create(privateKey).getAddress();
    }

    private boolean isCancelAlreadyRecorded(PredictionPolymarketOrder order) {
        String status = order.getStatus();
        if (status == null || status.isBlank()) {
            return false;
        }

        return CANCEL_IDEMPOTENT_STATUSES.stream()
                .anyMatch(value -> value.equalsIgnoreCase(status.trim()));
    }

    private boolean isUncertainCancelOutcome(Map<String, Object> raw) {
        String status =
                firstText(raw, "status");
        if ("EXCEPTION".equalsIgnoreCase(status)) {
            return true;
        }

        Object httpCode =
                raw.get("httpCode");
        if (httpCode == null) {
            return false;
        }

        try {
            return Integer.parseInt(httpCode.toString()) >= 500;
        } catch (Exception e) {
            return false;
        }
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

    private boolean compareDecimal(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }

        return left.compareTo(right) == 0;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
