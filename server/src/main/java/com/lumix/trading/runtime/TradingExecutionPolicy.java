package com.lumix.trading.runtime;

import java.math.BigDecimal;
import java.util.Objects;

/** 完整且版本化的執行政策；每個數值均由治理輸入指定，不從 sandbox 或外部價格推定。 */
public record TradingExecutionPolicy(TradingInstrumentRules rules, String feeAccountId, int maximumOpenOrders,
                                     BigDecimal maximumSlippage, FeeAccrual feeAccrual, CounterpartyModel counterpartyModel) {
    /** 累計未取整費用後才依固定政策取整，避免大量部分成交超過已預留的費用上界。 */
    public enum FeeAccrual { CUMULATIVE_QUOTE }
    /** 目前引擎只接受明確選擇的客戶互撮，政策未指定時不得啟用。 */
    public enum CounterpartyModel { CLIENT_TO_CLIENT }

    public TradingExecutionPolicy {
        Objects.requireNonNull(counterpartyModel, "counterparty model is required");
        Objects.requireNonNull(rules, "rules are required");
        Objects.requireNonNull(feeAccrual, "fee accrual policy is required");
        if (feeAccountId == null || feeAccountId.isBlank() || feeAccountId.length() > 64) throw new IllegalArgumentException("fee account is required");
        if (maximumOpenOrders < 1 || maximumOpenOrders > 1000) throw new IllegalArgumentException("open order limit is invalid");
        TradingDecimal.ledgerAmount(maximumSlippage);
        if (maximumSlippage.signum() <= 0 || maximumSlippage.compareTo(new BigDecimal("0.5")) >= 0) throw new IllegalArgumentException("explicit bounded price protection is required");
    }
}
