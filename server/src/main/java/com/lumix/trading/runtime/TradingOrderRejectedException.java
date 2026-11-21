package com.lumix.trading.runtime;

/** 預期的交易 gate 拒絕；與資料損毀、SQL 或 ledger invariant failure 分開，避免讓客戶無限重試無效委託。 */
public class TradingOrderRejectedException extends RuntimeException {
    public TradingOrderRejectedException(String reason){super(reason);}
}
