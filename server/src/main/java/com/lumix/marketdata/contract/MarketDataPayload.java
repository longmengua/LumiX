package com.lumix.marketdata.contract;

/**
 * Normalized event 唯一允許的 sealed payload 集合。
 *
 * <p>sealed boundary 是為了在 contract 層拒絕 provider-specific payload 直接滲入；adapter 若獲日後批准，
 * 也必須先轉換成這些中立型別。</p>
 */
public sealed interface MarketDataPayload permits BookSnapshotPayload, BookDeltaPayload, TradePayload, TickerPayload {

    MarketDataEventType eventType();

    void validateAgainst(InstrumentPrecision precision);

    String canonicalForm();
}
