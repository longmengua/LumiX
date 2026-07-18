package com.lumix.trading.core.futures.pnl;

import com.lumix.common.MoneyAmount;
import com.lumix.trading.core.futures.position.FuturesPosition;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Futures sandbox 的 pure、stateless PnL calculator。
 *
 * T04 只計算 isolated position 的價格差額，不包含 fee、funding、maintenance margin、rounding policy、balance mutation 或 ledger posting。
 */
public final class FuturesSandboxPnlCalculator {

    /**
     * 依外部傳入的單次 mark price 計算 unrealized PnL。
     *
     * LONG 使用 `(mark - entry) * quantity`；SHORT 使用 `(entry - mark) * quantity`，全程使用 BigDecimal 且不引入除法或 rounding。
     */
    public FuturesSandboxUnrealizedPnlSnapshot calculateUnrealized(FuturesSandboxUnrealizedPnlRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        MoneyAmount unrealizedPnl = new MoneyAmount(calculatePnl(
                request.position(),
                request.markPrice().value(),
                request.position().quantity().value()
        ));

        return new FuturesSandboxUnrealizedPnlSnapshot(
                request.position().positionId(),
                request.futuresAccount().settlementAsset(),
                request.markPrice(),
                unrealizedPnl,
                request.valuedAt()
        );
    }

    /**
     * 預覽指定 close price / quantity 下的 realized PnL，且不改變輸入 position。
     *
     * 此方法刻意不輸出 close event 或新的 position snapshot，避免 T04 越界實作 open-only T03 尚未允許的 position lifecycle runtime。
     */
    public FuturesSandboxRealizedPnlPreview previewRealized(FuturesSandboxRealizedPnlPreviewRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        MoneyAmount realizedPnl = new MoneyAmount(calculatePnl(
                request.position(),
                request.closePrice().value(),
                request.closeQuantity().value()
        ));

        return new FuturesSandboxRealizedPnlPreview(
                request.position().positionId(),
                request.futuresAccount().settlementAsset(),
                request.closePrice(),
                request.closeQuantity(),
                realizedPnl,
                request.previewedAt()
        );
    }

    private static BigDecimal calculatePnl(FuturesPosition position, BigDecimal exitPrice, BigDecimal quantity) {
        BigDecimal priceDifference = position.side().isLong()
                ? exitPrice.subtract(position.entryPrice().value())
                : position.entryPrice().value().subtract(exitPrice);
        return priceDifference.multiply(quantity);
    }
}
