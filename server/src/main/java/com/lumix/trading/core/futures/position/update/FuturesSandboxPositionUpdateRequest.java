package com.lumix.trading.core.futures.position.update;

import com.lumix.trading.core.futures.position.FuturesPosition;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Futures sandbox one-way、open-only position update 的 immutable 輸入。
 *
 * 已處理 fill 集合由呼叫端明確提供，讓 pure gate 能驗證重送風險但不偷接任何 idempotency store 或 persistence runtime。
 */
public record FuturesSandboxPositionUpdateRequest(
        FuturesSandboxVerifiedFill verifiedFill,
        Set<FuturesSandboxFillId> processedFillIds,
        Optional<FuturesPosition> existingBuyerPosition,
        Optional<FuturesPosition> existingSellerPosition
) {

    public FuturesSandboxPositionUpdateRequest {
        Objects.requireNonNull(verifiedFill, "verifiedFill must not be null");
        Objects.requireNonNull(processedFillIds, "processedFillIds must not be null");
        Objects.requireNonNull(existingBuyerPosition, "existingBuyerPosition must not be null");
        Objects.requireNonNull(existingSellerPosition, "existingSellerPosition must not be null");
        if (processedFillIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("processedFillIds must not contain null");
        }

        // Set.copyOf 避免 gate 評估期間被呼叫端修改，確保相同輸入一定得到相同決策。
        processedFillIds = Set.copyOf(processedFillIds);
    }
}
