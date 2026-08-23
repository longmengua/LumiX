package com.lumix.marketdata.aggregation;

import java.time.Instant;
import java.util.Objects;

/**
 * P21-T05 固定支援的 UTC candle interval。
 *
 * <p>interval 不採用呼叫端 runtime 設定，避免不同 replay 或 consumer 以同一事件建立不同 bucket。
 * 日後若需增加 interval，必須連同 query/transport contract 另行審核。</p>
 */
public enum CandleInterval {
    ONE_MINUTE(60),
    FIVE_MINUTES(300),
    ONE_HOUR(3_600);

    private final long seconds;

    CandleInterval(long seconds) {
        this.seconds = seconds;
    }

    /**
     * 以來源事件時間切出半開區間 [start, end)，不讀取本機 clock 或 received timestamp。
     */
    public CandleWindow windowFor(Instant sourceTimestamp) {
        sourceTimestamp = Objects.requireNonNull(sourceTimestamp, "sourceTimestamp must not be null");
        long startEpochSecond = Math.floorDiv(sourceTimestamp.getEpochSecond(), seconds) * seconds;
        Instant start = Instant.ofEpochSecond(startEpochSecond);
        return new CandleWindow(start, start.plusSeconds(seconds));
    }
}
