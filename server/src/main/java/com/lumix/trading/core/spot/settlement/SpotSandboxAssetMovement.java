package com.lumix.trading.core.spot.settlement;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Spot sandbox settlement 的單筆資產移動。
 *
 * 這份 record 只表達 settlement plan 內的意圖，不代表 DB write、ledger posting 或 reservation 已完成。
 */
public record SpotSandboxAssetMovement(
        SpotSandboxAssetMovementType movementType,
        String accountId,
        String assetSymbol,
        BigDecimal amount
) {

    /**
     * 建立不可變的 asset movement。
     *
     * 這裡只做最小 null 檢查，讓 settlement plan 在設計階段就保有可讀性與可追蹤性。
     */
    public SpotSandboxAssetMovement {
        Objects.requireNonNull(movementType, "movementType must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(assetSymbol, "assetSymbol must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
