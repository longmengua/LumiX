package com.lumix.marketfeed.binance;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.server.HandshakeInterceptor;

class BinanceDepthWebSocketTest {
    @Test
    void requiresExplicitHttpsOrigin() {
        // 未配置白名單時不可意外開啟跨站推播握手。
        assertThrows(IllegalArgumentException.class, () -> new BinanceDepthWebSocket(null, null, new ObjectMapper(), ""));
        assertThrows(IllegalArgumentException.class, () -> new BinanceDepthWebSocket(null, null, new ObjectMapper(), "*"));
        assertThrows(IllegalArgumentException.class, () -> new BinanceDepthWebSocket(null, null, new ObjectMapper(), "http://example.com"));
    }

    @Test
    void rejectsInsecureHandshake() throws Exception {
        // Nginx 未提供受信任的安全連線狀態時不得建立推播連線。
        Class<?> type = Class.forName("com.lumix.marketfeed.binance.BinanceDepthWebSocket$SecureHandshake");
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        HandshakeInterceptor interceptor = (HandshakeInterceptor) constructor.newInstance();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.beforeHandshake(new ServletServerHttpRequest(request), new ServletServerHttpResponse(response), null, new HashMap<>()));
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        request.setSecure(true);
        assertTrue(interceptor.beforeHandshake(new ServletServerHttpRequest(request), new ServletServerHttpResponse(new MockHttpServletResponse()), null, new HashMap<>()));
    }
}
