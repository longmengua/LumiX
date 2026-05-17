/*
 * 檔案用途：領域服務，封裝撮合、風控、Polymarket 同步與交易規則。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PolymarketUserWsEvent;
import com.example.exchange.domain.model.entity.PredictionPolymarketOrder;
import com.example.exchange.domain.model.entity.PredictionPolymarketWsEvent;
import com.example.exchange.domain.repository.jpa.PredictionPolymarketOrderRepository;
import com.example.exchange.domain.repository.jpa.PredictionPolymarketWsEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketUserEventService {

    private final ObjectMapper objectMapper;
    private final PredictionPolymarketOrderRepository orderRepository;
    private final PredictionPolymarketWsEventRepository eventRepository;

    @Transactional
    public void handle(PolymarketUserWsEvent event) {
        if (event == null) {
            return;
        }

        String eventKey =
                buildEventKey(event);

        if (eventRepository.findByEventKey(eventKey).isPresent()) {
            return;
        }

        PredictionPolymarketWsEvent entity =
                new PredictionPolymarketWsEvent();

        entity.setEventKey(eventKey);
        entity.setEventType(event.getEventType());
        entity.setStatus(event.getStatus());
        entity.setWalletAddress(event.getWalletAddress());
        entity.setMarket(event.getMarket());
        entity.setAssetId(event.getAssetId());
        entity.setOrderId(event.getOrderId());
        entity.setTradeId(event.getTradeId());
        entity.setPayload(toJson(event.getPayload()));
        entity.setReceivedAt(
                event.getReceivedAt() == null
                        ? LocalDateTime.now()
                        : LocalDateTime.ofInstant(event.getReceivedAt(), ZoneOffset.UTC)
        );

        eventRepository.save(entity);

        applyToOrder(event);
    }

    private void applyToOrder(PolymarketUserWsEvent event) {
        String orderId =
                event.getOrderId();

        if ((orderId == null || orderId.isBlank())
                && event.getPayload() != null) {
            orderId = firstText(event.getPayload(), "orderID", "orderId", "id", "taker_order_id");
        }

        if (orderId == null || orderId.isBlank()) {
            return;
        }

        orderRepository.findByClobOrderId(orderId)
                .ifPresent(order -> updateOrder(order, event));
    }

    private void updateOrder(
            PredictionPolymarketOrder order,
            PolymarketUserWsEvent event
    ) {
        if (event.getStatus() != null && !event.getStatus().isBlank()) {
            if ("trade".equalsIgnoreCase(event.getEventType())) {
                order.setTradeStatus(event.getStatus());
            } else {
                order.setStatus(event.getStatus());
            }
        }

        if (event.getTradeId() != null && !event.getTradeId().isBlank()) {
            order.setLastTradeId(event.getTradeId());
        }

        order.setLastClobPayload(toJson(event.getPayload()));
        order.setLastSyncedAt(LocalDateTime.now());

        orderRepository.save(order);
    }

    private String buildEventKey(PolymarketUserWsEvent event) {
        String eventType =
                safe(event.getEventType());
        String status =
                safe(event.getStatus());
        String orderId =
                safe(event.getOrderId());
        String tradeId =
                safe(event.getTradeId());
        String market =
                safe(event.getMarket());
        String assetId =
                safe(event.getAssetId());

        return eventType + ":" + status + ":" + orderId + ":" + tradeId + ":" + market + ":" + assetId;
    }

    private String firstText(
            Map<String, Object> payload,
            String... keys
    ) {
        for (String key : keys) {
            Object value =
                    payload.get(key);

            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }

        return null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
