package com.lumix.marketfeed.binance;

/**
 * 驗證 Binance 深度增量涵蓋的來源更新序號區間。
 *
 * <p>來源一筆訊息可能合併多次更新，不能把最後序號當成逐筆加一的事件序號。
 * 此純判斷不保存游標，也不執行網路或委託簿更新；呼叫端必須在同一交易對序列化判斷與套用。</p>
 */
public final class BinanceDepthSequence {
    private BinanceDepthSequence() { }

    /** 區分過期重送、可連續套用及必須重新取得快照的缺口。 */
    public enum Decision { OBSOLETE, APPLY, RESYNC_REQUIRED }

    /**
     * 依本地最後更新序號及來源區間決定能否套用；非法區間一律拒絕。
     * 第一筆與後續增量均須涵蓋本地游標之後的第一筆更新。
     */
    public static Decision evaluate(long lastApplied, long first, long last) {
        if (lastApplied < 0 || first < 0 || last < first) {
            throw new IllegalArgumentException("invalid depth update interval");
        }
        if (last <= lastApplied) {
            return Decision.OBSOLETE;
        }
        // 先排除 last <= lastApplied，避免游標為 Long.MAX_VALUE 時加一溢位。
        if (lastApplied == Long.MAX_VALUE || first > lastApplied + 1) {
            return Decision.RESYNC_REQUIRED;
        }
        return Decision.APPLY;
    }
}
