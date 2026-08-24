package com.lumix.marketdata.query;

import com.lumix.marketdata.contract.StreamKey;
import java.util.Optional;

/**
 * internal-only query port；重複查詢沒有 side effect，未知 stream 回傳空值而不合成零價格或假 live data。
 */
public interface ReadOnlyMarketDataQueryPort {

    Optional<MarketDataViewEnvelope> current(StreamKey streamKey);
}
