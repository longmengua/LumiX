package com.lumix.trading.core.futures.sandbox.market;

import com.lumix.trading.core.futures.account.FuturesAccount;
import com.lumix.trading.core.futures.pnl.FuturesSandboxPnlCalculator;
import com.lumix.trading.core.futures.pnl.FuturesSandboxPnlPrice;
import com.lumix.trading.core.futures.pnl.FuturesSandboxUnrealizedPnlRequest;
import com.lumix.trading.core.futures.pnl.FuturesSandboxUnrealizedPnlSnapshot;
import com.lumix.trading.core.futures.position.FuturesPosition;
import java.util.Objects;

/**
 * 將 T05 的 mock mark price 受限地轉交給 T04 PnL calculator 的 pure gate。
 *
 * 此 gate 不讀取行情、也不保存價格或部位；它只檢查 market 一致後組裝一次性 valuation request，
 * 讓 T04 保有既有的 account ownership 與 PnL 計算邊界。
 */
public final class FuturesSandboxMarkPricePnlValuationGate {

    private final FuturesSandboxPnlCalculator pnlCalculator;

    public FuturesSandboxMarkPricePnlValuationGate() {
        this(new FuturesSandboxPnlCalculator());
    }

    FuturesSandboxMarkPricePnlValuationGate(FuturesSandboxPnlCalculator pnlCalculator) {
        this.pnlCalculator = Objects.requireNonNull(pnlCalculator, "pnlCalculator must not be null");
    }

    /**
     * 以同 market 的 mock price 進行一次 unrealized PnL valuation。
     *
     * valuation 時間固定沿用 snapshot 的發布時間，避免此 sandbox 邊界自行讀取目前時間。
     */
    public FuturesSandboxUnrealizedPnlSnapshot calculateUnrealized(
            FuturesAccount futuresAccount,
            FuturesPosition position,
            FuturesSandboxMockMarkPrice markPrice
    ) {
        Objects.requireNonNull(futuresAccount, "futuresAccount must not be null");
        Objects.requireNonNull(position, "position must not be null");
        Objects.requireNonNull(markPrice, "markPrice must not be null");
        if (!position.marketSymbol().equals(markPrice.marketSymbol())) {
            throw new IllegalArgumentException("markPrice market must match position market");
        }

        return pnlCalculator.calculateUnrealized(new FuturesSandboxUnrealizedPnlRequest(
                futuresAccount,
                position,
                new FuturesSandboxPnlPrice(markPrice.price()),
                markPrice.publishedAt()
        ));
    }
}
