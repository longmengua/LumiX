package com.lumix.account.projection;

import java.time.Instant;

/**
 * 餘額 projection 的可用新鮮度證據。
 *
 * <p>read model 不能被誤認為 ledger source of truth；因此對外明確表示該列是否有不早於 projection
 * 時點的 reconciliation evidence。這個 enum 不會觸發 rebuild、修復或任何資金異動。</p>
 */
public enum BalanceProjectionFreshness {
    RECONCILED,
    UNRECONCILED;

    /**
     * 只有 reconciliation 時間存在且不早於 projection 時間，才可宣稱該列已有對帳證據。
     *
     * <p>採保守判斷：舊的 reconciledAt 在新的 projection 寫入後不再代表目前 row 已被對帳。</p>
     */
    static BalanceProjectionFreshness from(Instant projectedAt, Instant reconciledAt) {
        return reconciledAt != null && !reconciledAt.isBefore(projectedAt)
            ? RECONCILED
            : UNRECONCILED;
    }
}
