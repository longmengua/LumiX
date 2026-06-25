/*
 * 檔案用途：描述 WebSocket/SSE gateway runtime role 與連線狀態，供 readiness、LB drain 與營運查詢使用。
 */
package com.example.exchange.domain.model.dto;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class PushGatewayRuntimeStatus {

    private final String instanceId;

    private final String role;

    private final boolean acceptingNewStreams;

    private final boolean draining;

    private final int activeSseChannels;

    private final int activeSseSubscribers;

    private final int activeWebSocketChannels;

    private final int activeWebSocketSessions;
    public PushGatewayRuntimeStatus(String instanceId, String role, boolean acceptingNewStreams, boolean draining, int activeSseChannels, int activeSseSubscribers, int activeWebSocketChannels, int activeWebSocketSessions) {
        this.instanceId = instanceId;
        this.role = role;
        this.acceptingNewStreams = acceptingNewStreams;
        this.draining = draining;
        this.activeSseChannels = activeSseChannels;
        this.activeSseSubscribers = activeSseSubscribers;
        this.activeWebSocketChannels = activeWebSocketChannels;
        this.activeWebSocketSessions = activeWebSocketSessions;
    }

    public String instanceId() {
        return instanceId;
    }

    public String role() {
        return role;
    }

    public boolean acceptingNewStreams() {
        return acceptingNewStreams;
    }

    public boolean draining() {
        return draining;
    }

    public int activeSseChannels() {
        return activeSseChannels;
    }

    public int activeSseSubscribers() {
        return activeSseSubscribers;
    }

    public int activeWebSocketChannels() {
        return activeWebSocketChannels;
    }

    public int activeWebSocketSessions() {
        return activeWebSocketSessions;
    }
}