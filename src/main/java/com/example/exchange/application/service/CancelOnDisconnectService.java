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
    /** connectionId -> cancel-on-disconnect 註冊；目前為單 JVM MVP 狀態。 */
    private final Map<String, Registration> registrations = new ConcurrentHashMap<>();

    /**
     * 註冊一條 WebSocket 連線的斷線撤單設定。
     *
     * @param symbol 為 null 或空字串時代表撤掉該 uid 所有 open orders
     */
    public void register(String connectionId, long uid, String symbol) {
        if (connectionId == null || connectionId.isBlank()) {
            return;
        }
        registrations.put(connectionId, new Registration(uid, normalizeSymbol(symbol)));
    }

    /** WebSocket 關閉時觸發撤單，並移除一次性註冊。 */
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

    /** 測試與營運觀察用，回傳目前仍在記憶體中的註冊數。 */
    public int registeredCount() {
        return registrations.size();
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase();
    }

    /** 儲存單一連線要撤的使用者與可選 symbol 範圍。 */
    private record Registration(long uid, String symbol) {
    }
}
