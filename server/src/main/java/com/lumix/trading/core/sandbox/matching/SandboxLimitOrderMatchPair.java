package com.lumix.trading.core.sandbox.matching;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 共用限價單規則選出的 crossed pair。
 *
 * 此結果只描述兩個 order 在 sandbox 中具備配對資格；它不是 trade、fill、position change 或 settlement input。
 */
public record SandboxLimitOrderMatchPair(
        SandboxLimitOrderCandidate buyOrder,
        SandboxLimitOrderCandidate sellOrder,
        SandboxLimitOrderCandidate makerOrder,
        BigDecimal matchedQuantity
) {

    public SandboxLimitOrderMatchPair {
        Objects.requireNonNull(buyOrder, "buyOrder must not be null");
        Objects.requireNonNull(sellOrder, "sellOrder must not be null");
        Objects.requireNonNull(makerOrder, "makerOrder must not be null");
        Objects.requireNonNull(matchedQuantity, "matchedQuantity must not be null");

        if (buyOrder.side() != SandboxLimitOrderSide.BUY || sellOrder.side() != SandboxLimitOrderSide.SELL) {
            throw new IllegalArgumentException("match pair must contain BUY then SELL");
        }
        if (!buyOrder.marketSymbol().equals(sellOrder.marketSymbol())) {
            throw new IllegalArgumentException("match pair marketSymbol must match");
        }
        if (buyOrder.limitPrice().compareTo(sellOrder.limitPrice()) < 0) {
            throw new IllegalArgumentException("match pair requires crossed limit price");
        }
        if (!makerOrder.equals(buyOrder) && !makerOrder.equals(sellOrder)) {
            throw new IllegalArgumentException("makerOrder must be one of the matched orders");
        }
        if (matchedQuantity.signum() <= 0
                || matchedQuantity.compareTo(buyOrder.remainingQuantity()) > 0
                || matchedQuantity.compareTo(sellOrder.remainingQuantity()) > 0) {
            throw new IllegalArgumentException("matchedQuantity must fit both remaining quantities");
        }
    }
}
