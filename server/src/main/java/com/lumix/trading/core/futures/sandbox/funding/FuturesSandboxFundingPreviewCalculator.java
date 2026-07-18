package com.lumix.trading.core.futures.sandbox.funding;

import com.lumix.common.MoneyAmount;
import com.lumix.trading.core.futures.position.FuturesPosition;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Futures sandbox 的 pure、stateless funding 試算器。
 *
 * 計算線性 notional `markPrice * quantity * rate`。正 rate 下 LONG 應付、SHORT 應收；
 * 負 rate 會自然反轉方向。此類別從不寫入餘額、帳本、position 或任何 funding schedule 狀態。
 */
public final class FuturesSandboxFundingPreviewCalculator {

    /**
     * 依明確輸入的 mock price 與 rate 產生一次 funding preview。
     */
    public FuturesSandboxFundingPreview preview(FuturesSandboxFundingPreviewRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        FuturesPosition position = request.position();
        BigDecimal unsignedFunding = request.markPrice().price()
                .multiply(position.quantity().value())
                .multiply(request.fundingRate().value());
        BigDecimal signedFunding = position.side().isLong() ? unsignedFunding.negate() : unsignedFunding;

        return new FuturesSandboxFundingPreview(
                position.positionId(),
                request.futuresAccount().settlementAsset(),
                request.markPrice(),
                request.fundingRate(),
                new MoneyAmount(signedFunding),
                directionOf(signedFunding),
                request.fundingAt()
        );
    }

    private static FuturesSandboxFundingDirection directionOf(BigDecimal signedFunding) {
        if (signedFunding.signum() < 0) {
            return FuturesSandboxFundingDirection.PAY;
        }
        if (signedFunding.signum() > 0) {
            return FuturesSandboxFundingDirection.RECEIVE;
        }
        return FuturesSandboxFundingDirection.NONE;
    }
}
