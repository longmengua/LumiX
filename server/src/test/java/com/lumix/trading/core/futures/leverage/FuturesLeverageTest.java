package com.lumix.trading.core.futures.leverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 驗證 futures leverage 只接受正整數倍數，且具有穩定 value semantics。
 */
class FuturesLeverageTest {

    /**
     * 確認 1x 與一般正整數 leverage 都可建立。
     *
     * 這個 case 必須存在，因為 leverage 模型不能默認只容許高倍數或小數語意。
     */
    @Test
    void constructorAcceptsOneAndPositiveIntegers() {
        assertEquals(new FuturesLeverage(1), FuturesLeverage.of(1));
        assertEquals(new FuturesLeverage(7), FuturesLeverage.of(7));
        assertEquals(1, FuturesLeverage.of(1).multiplier());
        assertEquals(7, FuturesLeverage.of(7).multiplier());
    }

    /**
     * 確認 0 與負數都會被拒絕。
     *
     * 這個 case 必須存在，因為 leverage 是離散的正整數風險輸入，0 或負數沒有業務意義。
     */
    @Test
    void constructorRejectsZeroAndNegativeMultiplier() {
        assertThrows(IllegalArgumentException.class, () -> new FuturesLeverage(0));
        assertThrows(IllegalArgumentException.class, () -> new FuturesLeverage(-1));
    }

    /**
     * 確認 direct constructor 不能繞過 invariant，且 factory 與 constructor 的 value semantics 一致。
     *
     * 這個 case 必須存在，因為 canonical constructor 才是正式驗證邊界，factory 只是 convenience。
     */
    @Test
    void factoryAndConstructorShareValueSemantics() {
        FuturesLeverage fromFactory = FuturesLeverage.of(12);
        FuturesLeverage fromConstructor = new FuturesLeverage(12);

        assertEquals(fromFactory, fromConstructor);
        assertEquals(fromFactory.hashCode(), fromConstructor.hashCode());
    }

    /**
     * 確認相同 multiplier 的物件相等，不同 multiplier 的物件不相等。
     *
     * 這個 case 必須存在，因為 value object 必須有穩定 equality semantics，才能被 config 與測試正確比較。
     */
    @Test
    void valueEqualityDependsOnMultiplier() {
        assertEquals(new FuturesLeverage(3), new FuturesLeverage(3));
        assertNotEquals(new FuturesLeverage(3), new FuturesLeverage(4));
        assertTrue(new FuturesLeverage(3).equals(FuturesLeverage.of(3)));
    }
}
