package com.lumix.trading.core.futures.position.update;

import com.lumix.trading.core.futures.position.FuturesPosition;
import com.lumix.trading.core.futures.position.FuturesPositionId;
import com.lumix.trading.core.futures.position.FuturesPositionSide;
import java.util.Objects;

/**
 * Futures sandbox one-way、open-only 的 pure position opening gate。
 *
 * 此 gate 不修改既有 position、不接收 T02 candidate，也不寫入 database、reservation、ledger、PnL 或 settlement runtime。
 */
public final class FuturesSandboxPositionUpdateGate {

    /**
     * 依 verified fill 建立買方 LONG 與賣方 SHORT 的 immutable opening snapshots。
     *
     * P18-T03 刻意拒絕所有既有 position，讓加倉、減倉、平倉與反轉的價格及 PnL 語意保留給後續明確 task。
     */
    public FuturesSandboxPositionUpdateResult evaluate(FuturesSandboxPositionUpdateRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        FuturesSandboxVerifiedFill fill = request.verifiedFill();
        if (request.processedFillIds().contains(fill.fillId())) {
            return FuturesSandboxPositionUpdateResult.rejected(FuturesSandboxPositionUpdateReason.FILL_ALREADY_PROCESSED);
        }
        if (request.existingBuyerPosition().isPresent()) {
            if (!belongsToBuyerFillScope(request.existingBuyerPosition().orElseThrow(), fill)) {
                return FuturesSandboxPositionUpdateResult.rejected(
                        FuturesSandboxPositionUpdateReason.BUYER_POSITION_SCOPE_MISMATCH
                );
            }
            return FuturesSandboxPositionUpdateResult.rejected(
                    FuturesSandboxPositionUpdateReason.BUYER_POSITION_ALREADY_OPEN
            );
        }
        if (request.existingSellerPosition().isPresent()) {
            if (!belongsToSellerFillScope(request.existingSellerPosition().orElseThrow(), fill)) {
                return FuturesSandboxPositionUpdateResult.rejected(
                        FuturesSandboxPositionUpdateReason.SELLER_POSITION_SCOPE_MISMATCH
                );
            }
            return FuturesSandboxPositionUpdateResult.rejected(
                    FuturesSandboxPositionUpdateReason.SELLER_POSITION_ALREADY_OPEN
            );
        }

        return FuturesSandboxPositionUpdateResult.opened(
                FuturesPosition.open(
                        new FuturesPositionId("sandbox-position-" + fill.fillId().value() + "-buy"),
                        fill.buyOrder().futuresAccountId(),
                        fill.buyOrder().marketSymbol(),
                        FuturesPositionSide.LONG,
                        fill.fillQuantity(),
                        fill.fillPrice(),
                        fill.filledAt()
                ),
                FuturesPosition.open(
                        new FuturesPositionId("sandbox-position-" + fill.fillId().value() + "-sell"),
                        fill.sellOrder().futuresAccountId(),
                        fill.sellOrder().marketSymbol(),
                        FuturesPositionSide.SHORT,
                        fill.fillQuantity(),
                        fill.fillPrice(),
                        fill.filledAt()
                )
        );
    }

    private static boolean belongsToBuyerFillScope(FuturesPosition position, FuturesSandboxVerifiedFill fill) {
        return position.futuresAccountId().equals(fill.buyOrder().futuresAccountId())
                && position.marketSymbol().equals(fill.buyOrder().marketSymbol());
    }

    private static boolean belongsToSellerFillScope(FuturesPosition position, FuturesSandboxVerifiedFill fill) {
        return position.futuresAccountId().equals(fill.sellOrder().futuresAccountId())
                && position.marketSymbol().equals(fill.sellOrder().marketSymbol());
    }
}
