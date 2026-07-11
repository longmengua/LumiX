package com.lumix.trading.core.projection.runtime;

import java.time.Instant;
import java.util.Objects;

/**
 * balance projection rebuild 的結果。
 *
 * 這個結果只表示 read model 重建批次已完成，不代表 ledger、reservation 或 settlement runtime 已完成。
 */
public record BalanceProjectionRebuildResult(
        int projectedRowCount,
        long projectionVersion,
        Instant projectedAt
) {

    public BalanceProjectionRebuildResult {
        // 重建結果必須可追蹤，不能留下可變或不完整的時間資訊。
        if (projectedRowCount < 0) {
            throw new IllegalArgumentException("projectedRowCount must not be negative");
        }
        if (projectionVersion < 0L) {
            throw new IllegalArgumentException("projectionVersion must not be negative");
        }
        Objects.requireNonNull(projectedAt, "projectedAt must not be null");
    }
}
