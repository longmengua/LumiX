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
import org.springframework.dao.DataIntegrityViolationException;
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
    private final PolymarketOrderStateMachine orderStateMachine;

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
        String resolvedOrderId =
                resolveOrderId(event);
        String resolvedTradeId =
                resolveTradeId(event);

        entity.setEventKey(eventKey);
        entity.setEventType(event.getEventType());
        entity.setStatus(event.getStatus());
        entity.setWalletAddress(event.getWalletAddress());
        entity.setMarket(event.getMarket());
        entity.setAssetId(event.getAssetId());
        entity.setOrderId(resolvedOrderId);
        entity.setTradeId(resolvedTradeId);
        entity.setPayload(toJson(event.getPayload()));
        entity.setReceivedAt(
                event.getReceivedAt() == null
                        ? LocalDateTime.now()
                        : LocalDateTime.ofInstant(event.getReceivedAt(), ZoneOffset.UTC)
        );

        try {
            eventRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            log.info(
                    "[PolymarketUserEvent] Duplicate event replay ignored. eventKey={}",
                    eventKey
            );
            return;
        }

        applyToOrder(event);
    }

    private void applyToOrder(PolymarketUserWsEvent event) {
        String orderId =
                resolveOrderId(event);

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
            PolymarketOrderStateMachine.LifecycleTransition transition =
                    orderStateMachine.resolveUserEventStatus(
                            order.getStatus(),
                            order.getTradeStatus(),
                            event.getEventType(),
                            event.getStatus()
                    );
            order.setStatus(transition.orderStatus());
            order.setTradeStatus(transition.tradeStatus());
        }

        String tradeId =
                resolveTradeId(event);
        if (tradeId != null && !tradeId.isBlank()) {
            order.setLastTradeId(tradeId);
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
                safe(resolveOrderId(event));
        String tradeId =
                safe(resolveTradeId(event));
        String market =
                safe(event.getMarket());
        String assetId =
                safe(event.getAssetId());

        return eventType + ":" + status + ":" + orderId + ":" + tradeId + ":" + market + ":" + assetId;
    }

    private String resolveOrderId(PolymarketUserWsEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getOrderId() != null && !event.getOrderId().isBlank()) {
            return event.getOrderId().trim();
        }
        if (event.getPayload() == null) {
            return null;
        }
        return firstText(event.getPayload(), "orderID", "orderId", "order_id", "id", "taker_order_id");
    }

    private String resolveTradeId(PolymarketUserWsEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getTradeId() != null && !event.getTradeId().isBlank()) {
            return event.getTradeId().trim();
        }
        if (event.getPayload() == null) {
            return null;
        }
        return firstText(event.getPayload(), "tradeID", "tradeId", "trade_id");
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
