package com.lumix.trading.core.sandbox.matching;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Spot 與 futures 共用的 pure、stateless 限價單候選配對規則。
 *
 * 本 policy 只選出單一最佳 crossed pair，刻意不持有 book、不遞迴撮合、也不產生任何資產或狀態 side effect。
 */
public final class SandboxLimitOrderMatchingPolicy {

    /**
     * 從單一 market 的 active candidates 選出最佳 crossed pair。
     *
     * 呼叫端須先負責領域別的准入與 active-status 驗證；此處只固定價格、時間與 order ID 的 deterministic priority。
     */
    public Optional<SandboxLimitOrderMatchPair> selectBestCrossedPair(List<SandboxLimitOrderCandidate> candidates) {
        Objects.requireNonNull(candidates, "candidates must not be null");
        assertSingleMarketAndDistinctOrderIds(candidates);

        Optional<SandboxLimitOrderCandidate> bestBuy = candidates.stream()
                .filter(candidate -> candidate.side() == SandboxLimitOrderSide.BUY)
                .min(Comparator.comparing(SandboxLimitOrderCandidate::limitPrice).reversed()
                        .thenComparing(SandboxLimitOrderCandidate::acceptedAt)
                        .thenComparing(SandboxLimitOrderCandidate::orderId));
        Optional<SandboxLimitOrderCandidate> bestSell = candidates.stream()
                .filter(candidate -> candidate.side() == SandboxLimitOrderSide.SELL)
                .min(Comparator.comparing(SandboxLimitOrderCandidate::limitPrice)
                        .thenComparing(SandboxLimitOrderCandidate::acceptedAt)
                        .thenComparing(SandboxLimitOrderCandidate::orderId));

        if (bestBuy.isEmpty() || bestSell.isEmpty()
                || bestBuy.get().limitPrice().compareTo(bestSell.get().limitPrice()) < 0) {
            return Optional.empty();
        }

        SandboxLimitOrderCandidate makerOrder = compareMakerPriority(bestBuy.get(), bestSell.get()) <= 0
                ? bestBuy.get()
                : bestSell.get();
        return Optional.of(new SandboxLimitOrderMatchPair(
                bestBuy.get(),
                bestSell.get(),
                makerOrder,
                bestBuy.get().remainingQuantity().min(bestSell.get().remainingQuantity())
        ));
    }

    private static void assertSingleMarketAndDistinctOrderIds(List<SandboxLimitOrderCandidate> candidates) {
        Set<String> orderIds = new HashSet<>();
        String marketSymbol = null;
        for (SandboxLimitOrderCandidate candidate : candidates) {
            Objects.requireNonNull(candidate, "candidates must not contain null");
            if (!orderIds.add(candidate.orderId())) {
                throw new IllegalArgumentException("candidates must have distinct orderId values");
            }
            if (marketSymbol == null) {
                marketSymbol = candidate.marketSymbol();
            } else if (!marketSymbol.equals(candidate.marketSymbol())) {
                throw new IllegalArgumentException("candidates must belong to one marketSymbol");
            }
        }
    }

    private static int compareMakerPriority(SandboxLimitOrderCandidate left, SandboxLimitOrderCandidate right) {
        int acceptedAtCompare = left.acceptedAt().compareTo(right.acceptedAt());
        return acceptedAtCompare != 0 ? acceptedAtCompare : left.orderId().compareTo(right.orderId());
    }
}
