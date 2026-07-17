package com.lumix.trading.core.futures.order;

import com.lumix.trading.core.futures.margin.FuturesMarginCheckReason;
import com.lumix.trading.core.futures.margin.FuturesMarginCheckStatus;
import com.lumix.trading.core.futures.margin.IsolatedMarginCheckGate;
import com.lumix.trading.core.futures.margin.IsolatedMarginCheckResult;

import java.util.Objects;

/**
 * Futures sandbox order placement 的 pure、stateless、deterministic gate。
 *
 * 這個 gate 只負責核對 order request 與 margin proposal 是否完全一致，並重新執行 Phase 17 的 pure margin gate；
 * 它不接 clock、database、network、repository、matching engine、ledger、wallet、reservation 或 settlement runtime。
 */
public final class FuturesOrderPlacementGate {

    private final IsolatedMarginCheckGate marginCheckGate = new IsolatedMarginCheckGate();

    /**
     * 受理或拒絕單一 futures sandbox order placement request。
     *
     * evaluation order 固定為 account -> market -> leverage config -> proposal payload -> margin approval -> accepted snapshot，
     * 讓相同輸入永遠得到相同結果，且不會信任外部提供但無法證明關聯性的 approved margin result。
     */
    public FuturesOrderPlacementResult evaluate(FuturesOrderPlacementRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        if (!request.futuresAccountId().equals(request.marginCheckRequest().futuresAccount().accountId())) {
            return FuturesOrderPlacementResult.rejected(FuturesOrderPlacementReason.ACCOUNT_MISMATCH);
        }
        if (!request.marketSymbol().equals(request.marginCheckRequest().marketSymbol())) {
            return FuturesOrderPlacementResult.rejected(FuturesOrderPlacementReason.MARKET_MISMATCH);
        }
        if (!request.leverageConfig().equals(request.marginCheckRequest().leverageConfig())) {
            return FuturesOrderPlacementResult.rejected(FuturesOrderPlacementReason.MARGIN_PROPOSAL_MISMATCH);
        }
        if (!request.quantity().equals(request.marginCheckRequest().quantity())) {
            return FuturesOrderPlacementResult.rejected(FuturesOrderPlacementReason.MARGIN_PROPOSAL_MISMATCH);
        }
        if (!request.limitPrice().equals(request.marginCheckRequest().entryPrice())) {
            return FuturesOrderPlacementResult.rejected(FuturesOrderPlacementReason.MARGIN_PROPOSAL_MISMATCH);
        }

        IsolatedMarginCheckResult marginResult = marginCheckGate.check(request.marginCheckRequest());
        if (marginResult.status() != FuturesMarginCheckStatus.APPROVED
                || marginResult.reason() != FuturesMarginCheckReason.SUFFICIENT_MARGIN) {
            return FuturesOrderPlacementResult.rejected(FuturesOrderPlacementReason.MARGIN_CHECK_NOT_APPROVED);
        }

        FuturesSandboxOrder acceptedOrder = new FuturesSandboxOrder(
                request.orderId(),
                request.requestId(),
                request.futuresAccountId(),
                request.marketSymbol(),
                request.side(),
                request.type(),
                request.quantity(),
                request.limitPrice(),
                request.timeInForce(),
                request.leverageConfig().leverage(),
                // acceptedAt 直接沿用 submittedAt，避免在 placement gate 引入額外 clock dependency。
                request.submittedAt(),
                FuturesOrderStatus.ACCEPTED_FOR_SANDBOX,
                request.clientOrderId()
        );
        return FuturesOrderPlacementResult.accepted(acceptedOrder);
    }
}
