package com.lumix.common;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 金額值物件。
 * 只負責表示與比較金額，不負責資產異動或帳本計算。
 */
public record MoneyAmount(BigDecimal value) implements Comparable<MoneyAmount> {

    public MoneyAmount {
        // 金額為核心資料，建立時先檢查非空，避免後續運算出現隱性 NPE。
        Objects.requireNonNull(value, "value must not be null");
        // 去掉尾端 0，讓同值金額在字串與比較時表現一致。
        value = value.stripTrailingZeros();
    }

    /**
     * 回傳零金額，供 stub 與測試情境使用。
     */
    public static MoneyAmount zero() {
        return new MoneyAmount(BigDecimal.ZERO);
    }

    /**
     * 判斷金額是否大於 0。
     * Phase 9 的 transfer / ledger request 只允許正數。
     */
    public boolean isPositive() {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判斷金額是否小於 0。
     * 目前骨架只提供檢查，未在 Phase 9 使用負數資產異動。
     */
    public boolean isNegative() {
        return value.compareTo(BigDecimal.ZERO) < 0;
    }

    @Override
    public int compareTo(MoneyAmount other) {
        // 明確要求 other 非空，避免比較邏輯在呼叫端產生模糊錯誤。
        Objects.requireNonNull(other, "other must not be null");
        return value.compareTo(other.value);
    }
}
