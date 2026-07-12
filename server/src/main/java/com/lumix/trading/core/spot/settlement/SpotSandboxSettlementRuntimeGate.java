package com.lumix.trading.core.spot.settlement;

import com.lumix.trading.core.spot.matching.SpotSandboxSettlementInput;
import com.lumix.trading.core.spot.matching.SpotSandboxTradeFillStatus;
import com.lumix.ledger.domain.LedgerDirection;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Spot sandbox settlement runtime gate。
 *
 * 這個 gate 只把 settlement input 轉成 sandbox settlement plan 與 ledger posting candidate，不代表真正 settlement runtime 已完成。
 */
public final class SpotSandboxSettlementRuntimeGate {

    /**
     * 針對單一 settlement input 建立 sandbox settlement plan。
     *
     * 這裡只在 memory 內整理 candidate，不會呼叫 ledger posting gate、balance refresh 或任何 DB client。
     */
    public SpotSandboxSettlementRuntimeResult plan(SpotSandboxSettlementInput settlementInput) {
        Objects.requireNonNull(settlementInput, "settlementInput must not be null");

        if (settlementInput.status() != SpotSandboxTradeFillStatus.SETTLEMENT_NOT_STARTED) {
            return SpotSandboxSettlementRuntimeResult.rejected(
                    "SETTLEMENT_STATUS_NOT_ALLOWED",
                    "settlement input 必須仍停在 SETTLEMENT_NOT_STARTED"
            );
        }

        if (settlementInput.requestId() == null || settlementInput.requestId().isBlank()) {
            return SpotSandboxSettlementRuntimeResult.rejected(
                    "MISSING_REQUEST_ID",
                    "settlement input 缺少 requestId"
            );
        }
        if (settlementInput.idempotencyKey() == null || settlementInput.idempotencyKey().isBlank()) {
            return SpotSandboxSettlementRuntimeResult.rejected(
                    "MISSING_IDEMPOTENCY_KEY",
                    "settlement input 缺少 idempotencyKey"
            );
        }

        if (settlementInput.sandboxTradeId() == null || settlementInput.sandboxTradeId().isBlank()) {
            return SpotSandboxSettlementRuntimeResult.rejected(
                    "MISSING_SANDBOX_TRADE_ID",
                    "settlement input 缺少 sandboxTradeId"
            );
        }
        if (settlementInput.marketSymbol() == null || settlementInput.marketSymbol().isBlank()) {
            return SpotSandboxSettlementRuntimeResult.rejected(
                    "MISSING_MARKET_SYMBOL",
                    "settlement input 缺少 marketSymbol"
            );
        }
        if (settlementInput.buyAccountId() == null || settlementInput.buyAccountId().isBlank()) {
            return SpotSandboxSettlementRuntimeResult.rejected(
                    "MISSING_BUY_ACCOUNT_ID",
                    "settlement input 缺少 buyAccountId"
            );
        }
        if (settlementInput.sellAccountId() == null || settlementInput.sellAccountId().isBlank()) {
            return SpotSandboxSettlementRuntimeResult.rejected(
                    "MISSING_SELL_ACCOUNT_ID",
                    "settlement input 缺少 sellAccountId"
            );
        }
        if (settlementInput.price() == null || settlementInput.quantity() == null || settlementInput.quoteAmount() == null) {
            return SpotSandboxSettlementRuntimeResult.rejected(
                    "MISSING_AMOUNT_FIELDS",
                    "settlement input 缺少 price / quantity / quoteAmount"
            );
        }
        if (settlementInput.price().signum() <= 0 || settlementInput.quantity().signum() <= 0 || settlementInput.quoteAmount().signum() <= 0) {
            return SpotSandboxSettlementRuntimeResult.rejected(
                    "NON_POSITIVE_AMOUNT",
                    "price / quantity / quoteAmount 必須大於 0"
            );
        }

        MarketAssets marketAssets = parseMarketSymbol(settlementInput.marketSymbol());
        if (marketAssets == null) {
            return SpotSandboxSettlementRuntimeResult.rejected(
                    "INVALID_MARKET_SYMBOL",
                    "marketSymbol 必須能解析成 baseAsset-quoteAsset"
            );
        }

        BigDecimal expectedQuoteAmount = settlementInput.price().multiply(settlementInput.quantity());
        if (expectedQuoteAmount.compareTo(settlementInput.quoteAmount()) != 0) {
            return SpotSandboxSettlementRuntimeResult.rejected(
                    "QUOTE_AMOUNT_MISMATCH",
                    "quoteAmount 必須等於 price * quantity"
            );
        }

        List<SpotSandboxAssetMovement> movements = new ArrayList<>();
        movements.add(new SpotSandboxAssetMovement(
                SpotSandboxAssetMovementType.RECEIVE_BASE,
                settlementInput.buyAccountId(),
                marketAssets.baseAsset(),
                settlementInput.quantity()
        ));
        movements.add(new SpotSandboxAssetMovement(
                SpotSandboxAssetMovementType.PAY_QUOTE,
                settlementInput.buyAccountId(),
                marketAssets.quoteAsset(),
                settlementInput.quoteAmount()
        ));
        movements.add(new SpotSandboxAssetMovement(
                SpotSandboxAssetMovementType.PAY_BASE,
                settlementInput.sellAccountId(),
                marketAssets.baseAsset(),
                settlementInput.quantity()
        ));
        movements.add(new SpotSandboxAssetMovement(
                SpotSandboxAssetMovementType.RECEIVE_QUOTE,
                settlementInput.sellAccountId(),
                marketAssets.quoteAsset(),
                settlementInput.quoteAmount()
        ));

        SpotSandboxLedgerPostingCommandCandidate ledgerCandidate = new SpotSandboxLedgerPostingCommandCandidate(
                settlementInput.requestId(),
                settlementInput.idempotencyKey(),
                "SPOT_SANDBOX_SETTLEMENT",
                settlementInput.sandboxTradeId(),
                List.of(
                        new SpotSandboxLedgerPostingCommandCandidate.SpotSandboxLedgerPostingCommandLine(
                                settlementInput.buyAccountId(),
                                marketAssets.baseAsset(),
                                LedgerDirection.CREDIT,
                                settlementInput.quantity()
                        ),
                        new SpotSandboxLedgerPostingCommandCandidate.SpotSandboxLedgerPostingCommandLine(
                                settlementInput.buyAccountId(),
                                marketAssets.quoteAsset(),
                                LedgerDirection.DEBIT,
                                settlementInput.quoteAmount()
                        ),
                        new SpotSandboxLedgerPostingCommandCandidate.SpotSandboxLedgerPostingCommandLine(
                                settlementInput.sellAccountId(),
                                marketAssets.baseAsset(),
                                LedgerDirection.DEBIT,
                                settlementInput.quantity()
                        ),
                        new SpotSandboxLedgerPostingCommandCandidate.SpotSandboxLedgerPostingCommandLine(
                                settlementInput.sellAccountId(),
                                marketAssets.quoteAsset(),
                                LedgerDirection.CREDIT,
                                settlementInput.quoteAmount()
                        )
                )
        );

        SpotSandboxSettlementPlan plan = new SpotSandboxSettlementPlan(
                "sandbox-settlement-" + settlementInput.sandboxTradeId(),
                settlementInput.sandboxTradeId(),
                settlementInput.marketSymbol(),
                marketAssets.baseAsset(),
                marketAssets.quoteAsset(),
                settlementInput.buyAccountId(),
                settlementInput.sellAccountId(),
                settlementInput.price(),
                settlementInput.quantity(),
                settlementInput.quoteAmount(),
                movements,
                ledgerCandidate,
                SpotSandboxSettlementPlanStatus.PLANNED_FOR_SANDBOX,
                List.of(
                        SpotSandboxSettlementPlanFlag.LEDGER_NOT_POSTED,
                        SpotSandboxSettlementPlanFlag.BALANCE_NOT_UPDATED,
                        SpotSandboxSettlementPlanFlag.RESERVATION_NOT_COMMITTED,
                        SpotSandboxSettlementPlanFlag.RECONCILIATION_NOT_COMPLETED
                )
        );

        return SpotSandboxSettlementRuntimeResult.planned(
                plan,
                "sandbox settlement plan 已建立，但沒有執行 ledger posting / balance refresh / reservation commit"
        );
    }

    private static MarketAssets parseMarketSymbol(String marketSymbol) {
        if (marketSymbol == null) {
            return null;
        }

        String[] parts = marketSymbol.split("-", -1);
        if (parts.length != 2) {
            return null;
        }

        String baseAsset = parts[0].trim();
        String quoteAsset = parts[1].trim();
        if (baseAsset.isEmpty() || quoteAsset.isEmpty()) {
            return null;
        }

        return new MarketAssets(baseAsset, quoteAsset);
    }

    private record MarketAssets(String baseAsset, String quoteAsset) {
        private MarketAssets {
            Objects.requireNonNull(baseAsset, "baseAsset must not be null");
            Objects.requireNonNull(quoteAsset, "quoteAsset must not be null");
        }
    }
}
