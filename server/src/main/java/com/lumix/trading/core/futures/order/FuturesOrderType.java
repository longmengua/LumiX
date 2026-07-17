package com.lumix.trading.core.futures.order;

/**
 * Futures sandbox order type。
 *
 * T01 刻意只支援 LIMIT，因為 T02 matching reuse 尚未開始，MARKET / STOP 類型在沒有流動性與價格 runtime 前
 * 會形成無法正確執行的假語意。
 */
public enum FuturesOrderType {
    LIMIT
}
