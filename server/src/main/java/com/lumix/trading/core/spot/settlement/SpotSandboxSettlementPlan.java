package com.lumix.trading.core.spot.settlement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Spot sandbox settlement 的 plan。
 *
 * 這份 plan 只代表後續可能進入 ledger posting / balance refresh / reconciliation 的候選資料，不代表任何正式 runtime 已完成。
 */
public record SpotSandboxSettlementPlan(
        String sandboxSettlementId,
        String sandboxTradeId,
        String marketSymbol,
        String baseAsset,
        String quoteAsset,
        String buyAccountId,
        String sellAccountId,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal quoteAmount,
        List<SpotSandboxAssetMovement> movements,
        SpotSandboxLedgerPostingCommandCandidate ledgerPostingCommandCandidate,
        SpotSandboxSettlementPlanStatus status,
        List<SpotSandboxSettlementPlanFlag> flags
) {

    /**
     * 建立不可變的 settlement plan。
     *
     * 這裡只做必要 null 檢查，避免 sandbox plan 被意外建立成半成品。
     */
    public SpotSandboxSettlementPlan {
        Objects.requireNonNull(sandboxSettlementId, "sandboxSettlementId must not be null");
        Objects.requireNonNull(sandboxTradeId, "sandboxTradeId must not be null");
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
        Objects.requireNonNull(baseAsset, "baseAsset must not be null");
        Objects.requireNonNull(quoteAsset, "quoteAsset must not be null");
        Objects.requireNonNull(buyAccountId, "buyAccountId must not be null");
        Objects.requireNonNull(sellAccountId, "sellAccountId must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(quoteAmount, "quoteAmount must not be null");
        Objects.requireNonNull(movements, "movements must not be null");
        Objects.requireNonNull(ledgerPostingCommandCandidate, "ledgerPostingCommandCandidate must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(flags, "flags must not be null");
        movements = List.copyOf(movements);
        flags = List.copyOf(flags);
    }
}
