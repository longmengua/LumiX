/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class PushGatewayService {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L;
    public static final String HEARTBEAT_EVENT = "gateway.heartbeat";

    private final Map<String, Set<SseEmitter>> subscribers = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocketSession>> websocketSubscribers = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public PushGatewayService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribeMarket(String symbol) {
        return subscribe("market:" + normalize(symbol));
    }

    public SseEmitter subscribeUser(long uid) {
        return subscribe("user:" + uid);
    }

    public void publishMarket(String symbol, String eventName, Object payload) {
        publish("market:" + normalize(symbol), eventName, payload);
    }

    public void publishUser(long uid, String eventName, Object payload) {
        publish("user:" + uid, eventName, payload);
    }

    public PushGatewayHeartbeatReport publishHeartbeat(Instant now) {
        Instant heartbeatAt = now == null ? Instant.now() : now;
        Set<String> channels = new HashSet<>();
        channels.addAll(subscribers.keySet());
        channels.addAll(websocketSubscribers.keySet());

        int delivered = 0;
        int failed = 0;
        for (String channel : channels) {
            PublishResult result = publish(channel, HEARTBEAT_EVENT, new PushGatewayHeartbeat(channel, heartbeatAt.toString()));
            delivered += result.delivered();
            failed += result.failed();
        }
        return new PushGatewayHeartbeatReport(heartbeatAt, channels.size(), delivered, failed);
    }

    public void registerMarketWebSocket(String symbol, WebSocketSession session) {
        registerWebSocket("market:" + normalize(symbol), session);
    }

    public void registerUserWebSocket(long uid, WebSocketSession session) {
        registerWebSocket("user:" + uid, session);
    }

    public void removeWebSocket(WebSocketSession session) {
        websocketSubscribers.values().forEach(sessions -> sessions.remove(session));
    }

    private SseEmitter subscribe(String channel) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        subscribers.computeIfAbsent(channel, ignored -> new CopyOnWriteArraySet<>()).add(emitter);
        emitter.onCompletion(() -> remove(channel, emitter));
        emitter.onTimeout(() -> remove(channel, emitter));
        emitter.onError(ignored -> remove(channel, emitter));
        return emitter;
    }

    private PublishResult publish(String channel, String eventName, Object payload) {
        int delivered = 0;
        int failed = 0;
        Set<SseEmitter> emitters = subscribers.get(channel);
        if (emitters != null && !emitters.isEmpty()) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(payload));
                    delivered++;
                } catch (IOException | IllegalStateException ex) {
                    remove(channel, emitter);
                    failed++;
                }
            }
        }

        Set<WebSocketSession> sessions = websocketSubscribers.get(channel);
        if (sessions != null && !sessions.isEmpty()) {
            for (WebSocketSession session : sessions) {
                try {
                    if (!session.isOpen()) {
                        remove(channel, session);
                        failed++;
                        continue;
                    }
                    session.sendMessage(new TextMessage(toJson(eventName, payload)));
                    delivered++;
                } catch (IOException | IllegalStateException ex) {
                    remove(channel, session);
                    failed++;
                }
            }
        }
        return new PublishResult(delivered, failed);
    }

    private void registerWebSocket(String channel, WebSocketSession session) {
        websocketSubscribers.computeIfAbsent(channel, ignored -> new CopyOnWriteArraySet<>()).add(session);
    }

    private void remove(String channel, SseEmitter emitter) {
        Set<SseEmitter> emitters = subscribers.get(channel);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }

    private void remove(String channel, WebSocketSession session) {
        Set<WebSocketSession> sessions = websocketSubscribers.get(channel);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    private String toJson(String eventName, Object payload) throws IOException {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("event", eventName);
        message.put("data", payload);
        return objectMapper.writeValueAsString(message);
    }

    private static String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }

    public record PushGatewayHeartbeat(
            String channel,
            String ts
    ) {
    }

    public record PushGatewayHeartbeatReport(
            Instant heartbeatAt,
            int channels,
            int delivered,
            int failed
    ) {
    }

    private record PublishResult(
            int delivered,
            int failed
    ) {
    }
}
