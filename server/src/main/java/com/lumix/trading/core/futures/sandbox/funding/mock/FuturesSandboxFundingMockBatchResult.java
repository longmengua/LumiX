package com.lumix.trading.core.futures.sandbox.funding.mock;

import com.lumix.trading.core.futures.sandbox.funding.FuturesSandboxFundingPreview;
import java.util.List;
import java.util.Objects;

/**
 * Batch funding mock 的 immutable preview 集合。
 *
 * 每筆結果仍是 position 視角的獨立 preview；此結果不做跨帳戶 netting、付款或 settlement。
 */
public record FuturesSandboxFundingMockBatchResult(List<FuturesSandboxFundingPreview> previews) {

    public FuturesSandboxFundingMockBatchResult {
        Objects.requireNonNull(previews, "previews must not be null");
        previews = List.copyOf(previews);
        if (previews.isEmpty() || previews.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("previews must contain complete funding previews");
        }
    }
}
