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
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketPushConfig implements WebSocketConfigurer {

    private final PushGatewayService pushGatewayService;
    private final CancelOnDisconnectService cancelOnDisconnectService;
    private final MarketDataStreamRateLimiter marketDataStreamRateLimiter;
    private final UserStreamSubscriptionAuthorizer userStreamSubscriptionAuthorizer;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(marketHandler(), "/ws/market/*")
                .addInterceptors(rateLimitInterceptor("market"))
                .setAllowedOrigins("*");
        registry.addHandler(userHandler(), "/ws/user/*")
                .addInterceptors(rateLimitInterceptor("user"))
                .addInterceptors(userStreamAuthInterceptor())
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
