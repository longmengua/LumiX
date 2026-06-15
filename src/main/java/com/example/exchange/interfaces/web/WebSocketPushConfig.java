/*
 * 檔案用途：Java 原始碼檔案，屬於 java21-match-hub 交易服務。
 */
package com.example.exchange.interfaces.web;

import com.example.exchange.application.service.CancelOnDisconnectService;
import com.example.exchange.application.service.PushGatewayService;
import com.example.exchange.interfaces.web.security.MarketDataStreamRateLimiter;
import com.example.exchange.interfaces.web.security.UserStreamSubscriptionAuthorizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketPushConfig implements WebSocketConfigurer {

    private final PushGatewayService pushGatewayService;
    private final CancelOnDisconnectService cancelOnDisconnectService;
    private final MarketDataStreamRateLimiter marketDataStreamRateLimiter;
    private final UserStreamSubscriptionAuthorizer userStreamSubscriptionAuthorizer;
    private final ObjectMapper objectMapper;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(marketHandler(), "/ws/market/*")
                .addInterceptors(rateLimitInterceptor("market"))
                .setAllowedOrigins("*");
        registry.addHandler(userHandler(), "/ws/user/*")
                .addInterceptors(rateLimitInterceptor("user"))
                .addInterceptors(userStreamAuthInterceptor())
                .setAllowedOrigins("*");
        registry.addHandler(exchangeHandler(), "/ws/exchange")
                .addInterceptors(rateLimitInterceptor("exchange"))
                .setAllowedOrigins("*");
    }

    private WebSocketHandler marketHandler() {
        return new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                pushGatewayService.registerMarketWebSocket(lastPathSegment(session), session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                pushGatewayService.removeWebSocket(session);
                cancelOnDisconnectService.cancelForConnection(session.getId());
            }
        };
    }

    private WebSocketHandler userHandler() {
        return new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                long uid = Long.parseLong(lastPathSegment(session));
                pushGatewayService.registerUserWebSocket(uid, session);
                String resumeConnectionId = resumeConnectionId(session);
                if (resumeConnectionId != null) {
                    cancelOnDisconnectService.resume(resumeConnectionId, session.getId(), uid);
                }
                if (cancelOnDisconnectEnabled(session)) {
                    cancelOnDisconnectService.register(session.getId(), uid, cancelOnDisconnectSymbol(session));
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                pushGatewayService.removeWebSocket(session);
                cancelOnDisconnectService.cancelForConnection(session.getId());
            }
        };
    }

    WebSocketHandler exchangeHandler() {
        return new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
                JsonNode command = objectMapper.readTree(message.getPayload());
                String type = text(command, "type");
                switch (type) {
                    case "subscribe.market" -> subscribeMarket(session, command);
                    case "unsubscribe.market" -> unsubscribeMarket(session, command);
                    case "subscribe.user" -> subscribeUser(session, command);
                    case "unsubscribe.user" -> unsubscribeUser(session, command);
                    default -> sendControl(session, "error", Map.of("reason", "UNKNOWN_COMMAND", "type", type));
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                pushGatewayService.removeWebSocket(session);
                cancelOnDisconnectService.cancelForConnection(session.getId());
            }
        };
    }

    private void subscribeMarket(WebSocketSession session, JsonNode command) throws IOException {
        String symbol = text(command, "symbol");
        if (symbol.isBlank()) {
            sendControl(session, "error", Map.of("reason", "SYMBOL_REQUIRED"));
            return;
        }
        String previous = (String) session.getAttributes().put("exchange.market.symbol", symbol.trim().toUpperCase());
        if (previous != null && !previous.equalsIgnoreCase(symbol)) {
            pushGatewayService.unregisterMarketWebSocket(previous, session);
        }
        pushGatewayService.registerMarketWebSocket(symbol, session);
        sendControl(session, "subscribed.market", Map.of("symbol", symbol.trim().toUpperCase()));
    }

    private void unsubscribeMarket(WebSocketSession session, JsonNode command) throws IOException {
        String symbol = text(command, "symbol");
        if (!symbol.isBlank()) {
            pushGatewayService.unregisterMarketWebSocket(symbol, session);
        }
        session.getAttributes().remove("exchange.market.symbol");
        sendControl(session, "unsubscribed.market", Map.of("symbol", symbol.trim().toUpperCase()));
    }

    private void subscribeUser(WebSocketSession session, JsonNode command) throws IOException {
        String authorization = firstNonBlank(text(command, "authorization"), text(command, "token"));
        long authorizedUid = userStreamSubscriptionAuthorizer.resolveUid(text(command, "apiKey"), authorization);
        if (authorizedUid <= 0) {
            sendControl(session, "error", Map.of("reason", "USER_STREAM_AUTH_REQUIRED"));
            return;
        }
        String requestedUidText = text(command, "uid");
        long requestedUid = parseLongIfPresent(requestedUidText);
        if (requestedUid > 0 && requestedUid != authorizedUid) {
            sendControl(session, "error", Map.of("reason", "USER_STREAM_PERMISSION_DENIED"));
            return;
        }
        long uid = requestedUid > 0 ? requestedUid : authorizedUid;

        UserStreamSubscriptionAuthorizer.UserStreamAuthorizationDecision decision =
                userStreamSubscriptionAuthorizer.authorize(
                        uid,
                        firstNonBlank(text(command, "apiKey")),
                        authorization
                );
        if (!decision.allowed()) {
            sendControl(session, "error", Map.of("reason", decision.reason(), "status", decision.status().value()));
            return;
        }
        Long previous = (Long) session.getAttributes().put("exchange.user.uid", uid);
        if (previous != null && previous != uid) {
            pushGatewayService.unregisterUserWebSocket(previous, session);
        }
        pushGatewayService.registerUserWebSocket(uid, session);
        String resumeConnectionId = text(command, "resumeConnectionId");
        if (!resumeConnectionId.isBlank()) {
            cancelOnDisconnectService.resume(resumeConnectionId, session.getId(), uid);
        }
        if (command.path("cancelOnDisconnect").asBoolean(false)) {
            cancelOnDisconnectService.register(session.getId(), uid, text(command, "symbol"));
        }
        sendControl(session, "subscribed.user", Map.of("uid", uid, "connectionId", session.getId()));
    }

    private void unsubscribeUser(WebSocketSession session, JsonNode command) throws IOException {
        Object cached = session.getAttributes().get("exchange.user.uid");
        long uid = cached instanceof Long currentUid ? currentUid : 0L;
        long requestedUid = parseLongIfPresent(text(command, "uid"));
        long targetUid = requestedUid > 0 ? requestedUid : uid;
        if (targetUid > 0) {
            pushGatewayService.unregisterUserWebSocket(targetUid, session);
        }
        session.getAttributes().remove("exchange.user.uid");
        sendControl(session, "unsubscribed.user", Map.of("uid", targetUid));
    }

    private void sendControl(WebSocketSession session, String event, Object data) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", event);
        payload.put("data", data);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private static long parseLongIfPresent(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (RuntimeException ex) {
            return 0L;
        }
    }

    private static String lastPathSegment(WebSocketSession session) {
        return lastPathSegment(session.getUri());
    }

    private static String lastPathSegment(URI uri) {
        String path = uri == null ? "" : uri.getPath();
        int index = path.lastIndexOf('/');
        return index < 0 ? path : path.substring(index + 1);
    }

    private static boolean cancelOnDisconnectEnabled(WebSocketSession session) {
        return Boolean.parseBoolean(queryParams(session).getFirst("cancelOnDisconnect"));
    }

    private static String cancelOnDisconnectSymbol(WebSocketSession session) {
        return queryParams(session).getFirst("symbol");
    }

    private static String resumeConnectionId(WebSocketSession session) {
        String value = queryParams(session).getFirst("resumeConnectionId");
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static MultiValueMap<String, String> queryParams(WebSocketSession session) {
        return queryParams(session.getUri());
    }

    private static MultiValueMap<String, String> queryParams(URI uri) {
        if (uri == null) {
            return new LinkedMultiValueMap<>();
        }
        return UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams();
    }

    private HandshakeInterceptor userStreamAuthInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(
                    ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Map<String, Object> attributes
            ) {
                long uid;
                try {
                    uid = Long.parseLong(lastPathSegment(request.getURI()));
                } catch (NumberFormatException ex) {
                    response.setStatusCode(HttpStatus.BAD_REQUEST);
                    return false;
                }

                MultiValueMap<String, String> queryParams = queryParams(request.getURI());
                UserStreamSubscriptionAuthorizer.UserStreamAuthorizationDecision decision =
                        userStreamSubscriptionAuthorizer.authorize(
                                uid,
                                firstNonBlank(
                                        request.getHeaders().getFirst(userStreamSubscriptionAuthorizer.apiKeyHeaderName()),
                                        queryParams.getFirst("apiKey")
                                ),
                                firstNonBlank(
                                        request.getHeaders().getFirst("Authorization"),
                                        queryParams.getFirst("access_token"),
                                        queryParams.getFirst("token")
                                )
                        );
                if (!decision.allowed()) {
                    response.setStatusCode(decision.status());
                    return false;
                }
                return true;
            }

            @Override
            public void afterHandshake(
                    ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Exception exception
            ) {
            }
        };
    }

    private HandshakeInterceptor rateLimitInterceptor(String streamType) {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(
                    ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Map<String, Object> attributes
            ) {
                if (!pushGatewayService.acceptingNewStreams()) {
                    response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                    return false;
                }
                MarketDataStreamRateLimiter.RateLimitDecision decision =
                        marketDataStreamRateLimiter.consume(
                                request,
                                streamType,
                                lastPathSegment(request.getURI())
                        );
                if (!decision.allowed()) {
                    response.setStatusCode(decision.status());
                    return false;
                }
                return true;
            }

            @Override
            public void afterHandshake(
                    ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Exception exception
            ) {
            }
        };
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
