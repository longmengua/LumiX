package com.lumix.trading.core.sandbox.matching;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * 供共用限價單配對規則讀取的 immutable 候選快照。
 *
 * 此快照刻意只保存 price/time priority 所需資料，避免共用規則依賴任何特定市場的 order book、帳戶或資金狀態。
 */
public record SandboxLimitOrderCandidate(
        String orderId,
        String marketSymbol,
        SandboxLimitOrderSide side,
        BigDecimal limitPrice,
        BigDecimal remainingQuantity,
        Instant acceptedAt
) {

    public SandboxLimitOrderCandidate {
        // 共用規則沒有資料庫或上游 validation 可依賴，因此在邊界阻擋會破壞價時優先語意的半成品候選資料。
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
        Objects.requireNonNull(side, "side must not be null");
        Objects.requireNonNull(limitPrice, "limitPrice must not be null");
        Objects.requireNonNull(remainingQuantity, "remainingQuantity must not be null");
        Objects.requireNonNull(acceptedAt, "acceptedAt must not be null");

        orderId = requireNonBlank(orderId, "orderId");
        marketSymbol = requireNonBlank(marketSymbol, "marketSymbol");
        if (limitPrice.signum() <= 0) {
            throw new IllegalArgumentException("limitPrice must be positive");
        }
        if (remainingQuantity.signum() <= 0) {
            throw new IllegalArgumentException("remainingQuantity must be positive");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
