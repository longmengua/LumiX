package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PolymarketUserWsEvent;
import com.example.exchange.domain.model.dto.PolymarketUserWsStatusResponse;
import com.example.exchange.infra.config.PolymarketConfigs;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketUserWebSocketService {

    private static final String TOPIC = "polymarket.user.events";
    private static final String SOURCE = "POLYMARKET_USER_WS";

    private final ObjectMapper objectMapper;
    private final PolymarketConfigs polymarketConfigs;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread =
                        new Thread(r, "polymarket-user-ws");
                thread.setDaemon(true);
                return thread;
            });

    private final AtomicBoolean shouldRun =
            new AtomicBoolean(false);

    private final AtomicBoolean connecting =
            new AtomicBoolean(false);

    private final AtomicBoolean reconnectScheduled =
            new AtomicBoolean(false);

    private final AtomicReference<WebSocket> webSocketRef =
            new AtomicReference<>();

    private volatile ScheduledFuture<?> pingFuture;
    private volatile Instant connectedAt;
    private volatile Instant lastMessageAt;
    private volatile String lastEventType;
    private volatile String lastCloseReason;
    private volatile String lastError;

    @EventListener(ApplicationReadyEvent.class)
    public void autoStart() {
        if (Boolean.TRUE.equals(polymarketConfigs.getWs().getUserEnabled())) {
            startUserChannel();
        }
    }

    public synchronized PolymarketUserWsStatusResponse startUserChannel() {
        validateConfig();

        shouldRun.set(true);

        if (webSocketRef.get() != null || connecting.get()) {
            return status();
        }

        connect();

        return status();
    }

    public synchronized PolymarketUserWsStatusResponse stopUserChannel() {
        shouldRun.set(false);
        reconnectScheduled.set(false);
        connecting.set(false);

        stopPing();

        WebSocket webSocket =
                webSocketRef.getAndSet(null);

        if (webSocket != null) {
            webSocket.sendClose(
                    WebSocket.NORMAL_CLOSURE,
                    "manual stop"
            );
        }

        return status();
    }

    public PolymarketUserWsStatusResponse status() {
        return PolymarketUserWsStatusResponse.builder()
                .shouldRun(shouldRun.get())
                .connecting(connecting.get())
                .connected(webSocketRef.get() != null)
                .url(polymarketConfigs.getWs().getUserUrl())
                .walletAddress(polymarketConfigs.getWallet().getFunderAddress())
                .marketConditionIds(
                        List.copyOf(
                                polymarketConfigs
                                        .getWs()
                                        .getUserMarketConditionIds()
                        )
                )
                .connectedAt(connectedAt)
                .lastMessageAt(lastMessageAt)
                .lastEventType(lastEventType)
                .lastCloseReason(lastCloseReason)
                .lastError(lastError)
                .build();
    }

    private void connect() {
        connecting.set(true);
        lastError = null;

        httpClient.newWebSocketBuilder()
                .buildAsync(
                        URI.create(polymarketConfigs.getWs().getUserUrl()),
                        new UserChannelListener()
                )
                .whenComplete((webSocket, throwable) -> {
                    connecting.set(false);

                    if (throwable != null) {
                        lastError = throwable.getMessage();
                        log.warn(
                                "[PolymarketUserWS] connect failed: {}",
                                throwable.getMessage()
                        );
                        scheduleReconnect();
                    }
                });
    }

    private void sendSubscription(WebSocket webSocket) {
        ObjectNode root =
                objectMapper.createObjectNode();

        ObjectNode auth =
                root.putObject("auth");

        auth.put(
                "apiKey",
                polymarketConfigs.getClob().getApiKey()
        );
        auth.put(
                "secret",
                polymarketConfigs.getClob().getApiSecret()
        );
        auth.put(
                "passphrase",
                polymarketConfigs.getClob().getApiPassphrase()
        );

        root.put("type", "user");

        List<String> markets =
                polymarketConfigs
                        .getWs()
                        .getUserMarketConditionIds();

        if (markets != null && !markets.isEmpty()) {
            ArrayNode marketNodes =
                    root.putArray("markets");

            markets.forEach(marketNodes::add);
        }

        webSocket.sendText(root.toString(), true);
    }

    private void startPing(WebSocket webSocket) {
        stopPing();

        long intervalMs =
                Math.max(
                        1_000L,
                        polymarketConfigs.getWs().getPingIntervalMs()
                );

        pingFuture =
                scheduler.scheduleAtFixedRate(
                        () -> {
                            if (shouldRun.get() && webSocketRef.get() == webSocket) {
                                webSocket.sendText("PING", true);
                            }
                        },
                        intervalMs,
                        intervalMs,
                        TimeUnit.MILLISECONDS
                );
    }

    private void stopPing() {
        ScheduledFuture<?> future =
                pingFuture;

        if (future != null) {
            future.cancel(false);
            pingFuture = null;
        }
    }

    private void scheduleReconnect() {
        if (!shouldRun.get()) {
            return;
        }

        if (!reconnectScheduled.compareAndSet(false, true)) {
            return;
        }

        long delayMs =
                Math.max(
                        1_000L,
                        polymarketConfigs.getWs().getReconnectDelayMs()
                );

        scheduler.schedule(
                () -> {
                    reconnectScheduled.set(false);

                    if (shouldRun.get() && webSocketRef.get() == null) {
                        connect();
                    }
                },
                delayMs,
                TimeUnit.MILLISECONDS
        );
    }

    private void handleText(String text, WebSocket webSocket) {
        if (text == null || text.isBlank()) {
            return;
        }

        if ("PONG".equalsIgnoreCase(text.trim())) {
            return;
        }

        if ("PING".equalsIgnoreCase(text.trim())) {
            webSocket.sendText("PONG", true);
            return;
        }

        try {
            JsonNode root =
                    objectMapper.readTree(text);

            if (root.isArray()) {
                for (JsonNode node : root) {
                    publishEvent(node);
                }
                return;
            }

            publishEvent(root);
        } catch (Exception e) {
            lastError = e.getMessage();
            log.warn(
                    "[PolymarketUserWS] failed to parse message: {}",
                    e.getMessage()
            );
        }
    }

    private void publishEvent(JsonNode node) {
        if (node == null || !node.isObject()) {
            return;
        }

        String eventType =
                firstText(node, "event_type", "type", "eventType");

        if (eventType == null || eventType.isBlank()) {
            return;
        }

        Map<String, Object> payload =
                objectMapper.convertValue(
                        node,
                        new TypeReference<Map<String, Object>>() {
                        }
                );

        Instant now =
                Instant.now();

        PolymarketUserWsEvent event =
                PolymarketUserWsEvent.builder()
                        .source(SOURCE)
                        .eventType(eventType)
                        .status(firstText(node, "status", "trade_status"))
                        .walletAddress(polymarketConfigs.getWallet().getFunderAddress())
                        .market(firstText(node, "market", "condition_id"))
                        .assetId(firstText(node, "asset_id", "assetId"))
                        .orderId(firstText(node, "order_id", "id", "orderId"))
                        .tradeId(firstText(node, "trade_id", "tradeId"))
                        .receivedAt(now)
                        .payload(payload)
                        .build();

        kafkaTemplate.send(
                TOPIC,
                kafkaKey(event),
                event
        );

        lastMessageAt = now;
        lastEventType = eventType;
    }

    private String kafkaKey(PolymarketUserWsEvent event) {
        if (event.getOrderId() != null) {
            return event.getOrderId();
        }

        if (event.getTradeId() != null) {
            return event.getTradeId();
        }

        String walletAddress =
                event.getWalletAddress();

        if (walletAddress != null && !walletAddress.isBlank()) {
            return walletAddress;
        }

        return event.getEventType();
    }

    private String firstText(
            JsonNode node,
            String... fieldNames
    ) {
        for (String fieldName : fieldNames) {
            JsonNode value =
                    node.get(fieldName);

            if (value != null && !value.isNull()) {
                String text =
                        value.asText();

                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }

        return null;
    }

    private void validateConfig() {
        requireNonBlank(
                polymarketConfigs.getClob().getApiKey(),
                "polymarket.clob.api-key is required"
        );
        requireNonBlank(
                polymarketConfigs.getClob().getApiSecret(),
                "polymarket.clob.api-secret is required"
        );
        requireNonBlank(
                polymarketConfigs.getClob().getApiPassphrase(),
                "polymarket.clob.api-passphrase is required"
        );
    }

    private void requireNonBlank(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    @PreDestroy
    public void shutdown() {
        stopUserChannel();
        scheduler.shutdownNow();
    }

    private class UserChannelListener implements WebSocket.Listener {

        private final StringBuilder textBuffer =
                new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocketRef.set(webSocket);
            connectedAt = Instant.now();
            lastCloseReason = null;

            sendSubscription(webSocket);
            startPing(webSocket);

            log.info("[PolymarketUserWS] connected");

            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(
                WebSocket webSocket,
                CharSequence data,
                boolean last
        ) {
            textBuffer.append(data);

            if (last) {
                String text =
                        textBuffer.toString();

                textBuffer.setLength(0);
                handleText(text, webSocket);
            }

            webSocket.request(1);

            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(
                WebSocket webSocket,
                int statusCode,
                String reason
        ) {
            if (webSocketRef.compareAndSet(webSocket, null)) {
                stopPing();
            }

            lastCloseReason =
                    statusCode + ":" + reason;

            log.info(
                    "[PolymarketUserWS] closed. code={}, reason={}",
                    statusCode,
                    reason
            );

            scheduleReconnect();

            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(
                WebSocket webSocket,
                Throwable error
        ) {
            if (webSocketRef.compareAndSet(webSocket, null)) {
                stopPing();
            }

            lastError =
                    error.getMessage();

            log.warn(
                    "[PolymarketUserWS] error: {}",
                    error.getMessage()
            );

            scheduleReconnect();
        }
    }
}
