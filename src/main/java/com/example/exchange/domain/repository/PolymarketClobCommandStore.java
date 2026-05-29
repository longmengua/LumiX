/*
 * 檔案用途：Repository 介面，定義 Polymarket CLOB command idempotency claim/result 儲存契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.PolymarketClobCommandRecord;

import java.util.Optional;

public interface PolymarketClobCommandStore {

    Optional<PolymarketClobCommandRecord> find(String commandId);

    boolean claim(
            String commandId,
            String commandType,
            String internalOrderId,
            String fingerprint
    );

    PolymarketClobCommandRecord complete(
            String commandId,
            String resultStatus,
            String lastError
    );
}
