package com.lumix.trading.liquidity;

import com.lumix.trading.runtime.TradingInstrumentRules;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 外部深度的讀取邊界。公開行情只代表來源當時的報價，不能當成已預留或保證可成交的數量。
 * 實際來源 adapter 須先驗證商品、來源序號與時效，才可回傳完整快照。
 */
public interface ExternalLiquiditySource {
    /** 缺少連續、有效的來源快照時回傳 empty；不得沿用上一筆深度。 */
    Optional<Snapshot> snapshot(TradingInstrumentRules.Kind kind, String marketSymbol);

    record Level(BigDecimal price, BigDecimal quantity) { }

    record Snapshot(String source, TradingInstrumentRules.Kind kind, String marketSymbol,
                    Instant sourceAt, Instant receivedAt, List<Level> bids, List<Level> asks) { }
}
