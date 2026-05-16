package com.example.exchange.domain.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Polymarket user WebSocket event.
 */
@Data
@Builder
public class PolymarketUserWsEvent {

    private String source;

    private String eventType;

    private String status;

    private String walletAddress;

    private String market;

    private String assetId;

    private String orderId;

    private String tradeId;

    private Instant receivedAt;

    private Map<String, Object> payload;
}
