/*
 * 檔案用途：保存 Polymarket CLOB effectful command 的 idempotency 狀態。
 */
package com.example.exchange.domain.model.dto;

public record PolymarketClobCommandRecord(
        String commandId,
        String commandType,
        String internalOrderId,
        String fingerprint,
        boolean completed,
        String resultStatus,
        String lastError
) {
}
