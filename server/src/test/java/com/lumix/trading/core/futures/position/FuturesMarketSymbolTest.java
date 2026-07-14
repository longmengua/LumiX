package com.lumix.trading.core.futures.position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * 驗證 futures 專用 market symbol identity 只保留必要的正規化規則。
 */
class FuturesMarketSymbolTest {

    /**
     * 確認 symbol 會 trim 並轉成大寫，避免相同交易對被大小寫與空白拆成不同 identity。
     *
     * 這個 case 必須存在，因為 position 只需要穩定 identity，不需要展示模型的額外欄位。
     */
    @Test
    void constructorNormalizesSymbolIdentity() {
        FuturesMarketSymbol symbol = new FuturesMarketSymbol("  btc-usdt  ");

        assertEquals(new FuturesMarketSymbol("BTC-USDT"), symbol);
    }

    /**
     * 確認 symbol 不接受空值與空白，避免 position identity 出現不可追蹤的半成品。
     */
    @Test
    void constructorRejectsNullAndBlankSymbol() {
        assertThrows(NullPointerException.class, () -> new FuturesMarketSymbol(null));
        assertThrows(IllegalArgumentException.class, () -> new FuturesMarketSymbol("   "));
    }
}
