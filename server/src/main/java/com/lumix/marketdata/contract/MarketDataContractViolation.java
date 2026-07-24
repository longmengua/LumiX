package com.lumix.marketdata.contract;

import java.util.Objects;

/**
 * 將 fail-closed 驗證結果保留成固定 reason code 的例外。
 *
 * <p>這不是 API error；P21-T02 尚未建立 transport。保留 reason 的目的，是讓未來 adapter 與 health
 * policy 可安全分類拒絕事件，而不依賴不穩定的訊息文字。</p>
 */
public final class MarketDataContractViolation extends IllegalArgumentException {

    private final MarketDataRejectionReason reason;

    public MarketDataContractViolation(MarketDataRejectionReason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public MarketDataRejectionReason reason() {
        return reason;
    }
}
