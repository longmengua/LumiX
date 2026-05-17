/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Polymarket user WebSocket runtime status.
 */
@Data
@Builder
public class PolymarketUserWsStatusResponse {

    private boolean shouldRun;

    private boolean connecting;

    private boolean connected;

    private String url;

    private String walletAddress;

    private List<String> marketConditionIds;

    private Instant connectedAt;

    private Instant lastMessageAt;

    private String lastEventType;

    private String lastCloseReason;

    private String lastError;
}
