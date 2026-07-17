package com.lumix.trading.core.futures.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * 驗證 futures order identity 只接受穩定且不可為空白的值。
 */
class FuturesOrderIdTest {

    /**
     * 確認 order ID 會 trim 並保留 value equality。
     *
     * 這個 case 必須存在，因為 order ID 會作為 placement、matching 與審計的穩定識別碼。
     */
    @Test
    void constructorNormalizesAndPreservesValueEquality() {
        FuturesOrderId orderId = new FuturesOrderId("  fut-order-001  ");

        assertEquals(new FuturesOrderId("fut-order-001"), orderId);
    }

    /**
     * 確認 null 與空白 ID 都會被拒絕。
     */
    @Test
    void constructorRejectsNullAndBlankValue() {
        assertThrows(NullPointerException.class, () -> new FuturesOrderId(null));
        assertThrows(IllegalArgumentException.class, () -> new FuturesOrderId("   "));
    }
}
