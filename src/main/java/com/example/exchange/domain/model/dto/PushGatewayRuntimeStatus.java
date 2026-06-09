/*
 * 檔案用途：描述 WebSocket/SSE gateway runtime role 與連線狀態，供 readiness、LB drain 與營運查詢使用。
 */
package com.example.exchange.domain.model.dto;

public record PushGatewayRuntimeStatus(
        String instanceId,
        String role,
        boolean acceptingNewStreams,
        boolean draining,
        int activeSseChannels,
        int activeSseSubscribers,
        int activeWebSocketChannels,
        int activeWebSocketSessions
) {
}
