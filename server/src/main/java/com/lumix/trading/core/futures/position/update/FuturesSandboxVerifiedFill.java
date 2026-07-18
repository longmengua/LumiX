package com.lumix.trading.core.futures.position.update;

import com.lumix.trading.core.futures.order.FuturesOrderSide;
import com.lumix.trading.core.futures.order.FuturesOrderStatus;
import com.lumix.trading.core.futures.order.FuturesSandboxOrder;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import java.time.Instant;
import java.util.Objects;

/**
 * 已由 T03 外部受信邊界驗證的 sandbox fill 輸入快照。
 *
 * 此型別沒有從 T02 candidate 建立的 factory，因為 crossed limit price 只代表候選資格，不能被直接誤當成已成交。
 */
public record FuturesSandboxVerifiedFill(
        FuturesSandboxFillId fillId,
        FuturesSandboxOrder buyOrder,
        FuturesSandboxOrder sellOrder,
        FuturesEntryPrice fillPrice,
        FuturesPositionQuantity fillQuantity,
        Instant filledAt
) {

    public FuturesSandboxVerifiedFill {
        // Position opening 的唯一事實來源必須是已驗證 fill，而非 accepted order 或 candidate，避免產生無成交依據的幽靈部位。
        Objects.requireNonNull(fillId, "fillId must not be null");
        Objects.requireNonNull(buyOrder, "buyOrder must not be null");
        Objects.requireNonNull(sellOrder, "sellOrder must not be null");
        Objects.requireNonNull(fillPrice, "fillPrice must not be null");
        Objects.requireNonNull(fillQuantity, "fillQuantity must not be null");
        Objects.requireNonNull(filledAt, "filledAt must not be null");

        if (buyOrder.status() != FuturesOrderStatus.ACCEPTED_FOR_SANDBOX
                || sellOrder.status() != FuturesOrderStatus.ACCEPTED_FOR_SANDBOX) {
            throw new IllegalArgumentException("verified fill orders must be accepted for sandbox");
        }
        if (buyOrder.side() != FuturesOrderSide.BUY || sellOrder.side() != FuturesOrderSide.SELL) {
            throw new IllegalArgumentException("verified fill must contain BUY then SELL orders");
        }
        if (!buyOrder.marketSymbol().equals(sellOrder.marketSymbol())) {
            throw new IllegalArgumentException("verified fill orders must share marketSymbol");
        }
        if (buyOrder.futuresAccountId().equals(sellOrder.futuresAccountId())) {
            throw new IllegalArgumentException("verified fill must not self-match one futures account");
        }
        if (fillPrice.value().compareTo(buyOrder.limitPrice().value()) > 0
                || fillPrice.value().compareTo(sellOrder.limitPrice().value()) < 0) {
            throw new IllegalArgumentException("fillPrice must remain within both order limits");
        }
        if (fillQuantity.value().compareTo(buyOrder.quantity().value()) > 0
                || fillQuantity.value().compareTo(sellOrder.quantity().value()) > 0) {
            throw new IllegalArgumentException("fillQuantity must not exceed either order quantity");
        }
        if (filledAt.isBefore(buyOrder.acceptedAt()) || filledAt.isBefore(sellOrder.acceptedAt())) {
            throw new IllegalArgumentException("filledAt must not be before order acceptance");
        }
    }
}
