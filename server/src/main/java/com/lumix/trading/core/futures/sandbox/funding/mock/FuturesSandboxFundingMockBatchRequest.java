package com.lumix.trading.core.futures.sandbox.funding.mock;

import com.lumix.trading.core.futures.sandbox.funding.FuturesSandboxFundingPreviewRequest;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 單一 market、單一 rate 與單一 funding time 的 batch mock scenario。
 *
 * 此型別將多筆既有 preview input 限制在同一個可重放的 sandbox funding cycle，
 * 但不會將 preview 視為付款指令或保存任何 cycle 狀態。
 */
public record FuturesSandboxFundingMockBatchRequest(List<FuturesSandboxFundingPreviewRequest> previews) {

    public FuturesSandboxFundingMockBatchRequest {
        Objects.requireNonNull(previews, "previews must not be null");
        previews = List.copyOf(previews);
        if (previews.isEmpty()) {
            throw new IllegalArgumentException("previews must not be empty");
        }
        if (previews.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("previews must not contain null");
        }
        FuturesSandboxFundingPreviewRequest first = previews.getFirst();
        if (previews.stream().anyMatch(preview -> !first.position().marketSymbol().equals(preview.position().marketSymbol()))) {
            throw new IllegalArgumentException("all previews must use the same market");
        }
        if (previews.stream().anyMatch(preview -> !first.fundingRate().equals(preview.fundingRate()))) {
            throw new IllegalArgumentException("all previews must use the same fundingRate");
        }
        Instant fundingAt = first.fundingAt();
        if (previews.stream().anyMatch(preview -> !fundingAt.equals(preview.fundingAt()))) {
            throw new IllegalArgumentException("all previews must use the same fundingAt");
        }
        Set<?> positionIds = previews.stream().map(preview -> preview.position().positionId()).collect(java.util.stream.Collectors.toSet());
        if (positionIds.size() != previews.size()) {
            throw new IllegalArgumentException("previews must not contain duplicate positionId");
        }
    }
}
