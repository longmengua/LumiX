package com.lumix.trading.core.futures.sandbox.contract;

import com.lumix.trading.core.futures.order.FuturesOrderPlacementResult;
import com.lumix.trading.core.futures.order.FuturesSandboxOrder;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPrice;
import java.util.Objects;

/**
 * Phase 18 受限 futures contract sandbox 的 pure eligibility gate。
 *
 * 此 gate 只核對單一 contract market、accepted order 與人工 mark-price snapshot 的一致性；
 * 它刻意不呼叫 matching、verified fill、position update、PnL、funding、reservation、ledger 或 settlement 邊界。
 */
public final class FuturesSandboxRestrictedContractGate {

    /**
     * 判斷 T01 placement 結果與 mock mark price 是否可供指定的單一 sandbox contract 檢視。
     *
     * 成功結果僅為 inspection eligibility；T06 不提供 candidate-to-fill、下單執行、開倉、付款或結算能力。
     */
    public FuturesSandboxContractEligibilityResult evaluate(
            RestrictedFuturesSandboxContract contract,
            FuturesOrderPlacementResult placementResult,
            FuturesSandboxMockMarkPrice markPrice
    ) {
        Objects.requireNonNull(contract, "contract must not be null");
        Objects.requireNonNull(placementResult, "placementResult must not be null");
        Objects.requireNonNull(markPrice, "markPrice must not be null");
        if (placementResult.acceptedOrder().isEmpty()) {
            return FuturesSandboxContractEligibilityResult.rejected(
                    FuturesSandboxContractEligibilityReason.ORDER_NOT_ACCEPTED_FOR_SANDBOX
            );
        }
        FuturesSandboxOrder order = placementResult.acceptedOrder().orElseThrow();
        if (!contract.marketSymbol().equals(order.marketSymbol())) {
            return FuturesSandboxContractEligibilityResult.rejected(
                    FuturesSandboxContractEligibilityReason.ORDER_MARKET_OUTSIDE_CONTRACT
            );
        }
        if (!contract.marketSymbol().equals(markPrice.marketSymbol())) {
            return FuturesSandboxContractEligibilityResult.rejected(
                    FuturesSandboxContractEligibilityReason.MARK_PRICE_MARKET_OUTSIDE_CONTRACT
            );
        }

        return FuturesSandboxContractEligibilityResult.eligible(
                new FuturesSandboxContractInspection(contract, order, markPrice)
        );
    }
}
