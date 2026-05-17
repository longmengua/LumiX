/*
 * 檔案用途：應用服務，管理 WebSocket 斷線撤單註冊與觸發。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.usecase.CancelOrderUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CancelOnDisconnectService {

    private final CancelOrderUseCase cancelOrderUseCase;
    private final Map<String, Registration> registrations = new ConcurrentHashMap<>();

    public void register(String connectionId, long uid, String symbol) {
        if (connectionId == null || connectionId.isBlank()) {
            return;
        }
        registrations.put(connectionId, new Registration(uid, normalizeSymbol(symbol)));
    }

    public int cancelForConnection(String connectionId) {
        if (connectionId == null || connectionId.isBlank()) {
            return 0;
        }
        Registration registration = registrations.remove(connectionId);
        if (registration == null) {
            return 0;
        }
        int canceled = cancelOrderUseCase.cancelOpenOrders(registration.uid(), registration.symbol());
        log.info(
                "CANCEL_ON_DISCONNECT uid={} symbol={} canceled={}",
                registration.uid(),
                registration.symbol() == null ? "ALL" : registration.symbol(),
                canceled
        );
        return canceled;
    }

    public int registeredCount() {
        return registrations.size();
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase();
    }

    private record Registration(long uid, String symbol) {
    }
}
