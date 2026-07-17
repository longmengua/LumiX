package com.lumix.trading.core.futures.order;

import java.util.Objects;

/**
 * Futures sandbox order 的穩定識別碼。
 *
 * 這個 ID 只負責識別單一 order，不連接資料庫、不呼叫 ID generator，也不承載任何 matching / settlement runtime 語意。
 */
public record FuturesOrderId(String value) {

    public FuturesOrderId {
        // Order ID 是 placement 後續追蹤與審計的主鍵，因此在 value object 邊界先阻擋 null 與空白。
        Objects.requireNonNull(value, "value must not be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("futuresOrderId must not be blank");
        }
    }
}
