package com.lumix.trading.core.futures.position;

import java.util.Objects;

/**
 * Futures position 的穩定識別碼。
 *
 * 這個 ID 只負責識別單一 position，不承載任何 margin pool 或 account aggregation 資訊。
 */
public record FuturesPositionId(String value) {

    public FuturesPositionId {
        // Position ID 會被後續 position / risk / reconciliation 流程反覆引用，因此先保證不是空字串。
        Objects.requireNonNull(value, "value must not be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("positionId must not be blank");
        }
    }
}
