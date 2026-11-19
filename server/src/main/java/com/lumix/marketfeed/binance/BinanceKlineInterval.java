package com.lumix.marketfeed.binance;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;

/** 具名週期白名單；週線與月線依 UTC 曆法處理，不能以固定三十天替代月份。 */
public enum BinanceKlineInterval {
    MINUTE_1("1m", 60_000), MINUTE_5("5m", 300_000), MINUTE_15("15m", 900_000),
    HOUR_1("1h", 3_600_000), DAY_1("1d", 86_400_000), WEEK_1("1w", 0), MONTH_1("1M", 0);

    private final String value;
    private final long millis;
    BinanceKlineInterval(String value, long millis) { this.value = value; this.millis = millis; }

    /** 僅供固定 Binance REST／WebSocket 來源使用，不接受任意路徑或 interval。 */
    public String value() { return value; }

    /** 大小寫有意義：1m 為分鐘，1M 為曆月。 */
    public static BinanceKlineInterval parse(String value) {
        for (BinanceKlineInterval interval : values()) if (interval.value.equals(value)) return interval;
        throw new IllegalArgumentException("unsupported kline interval");
    }

    /** 計算下一根開盤時間；呼叫端先驗證本根是否對齊來源週期。 */
    public long nextOpen(long openTime) {
        if (millis > 0) return Math.addExact(openTime, millis);
        var utc = Instant.ofEpochMilli(openTime).atZone(ZoneOffset.UTC);
        return (this == MONTH_1 ? utc.plusMonths(1) : utc.plusWeeks(1)).toInstant().toEpochMilli();
    }

    /** Binance UTC 週線以星期一開盤，月線以每月一日開盤。 */
    public boolean aligned(long openTime) {
        if (openTime < 0) return false;
        if (millis > 0) return openTime % millis == 0;
        var utc = Instant.ofEpochMilli(openTime).atZone(ZoneOffset.UTC);
        boolean midnight = utc.toLocalTime().equals(java.time.LocalTime.MIDNIGHT);
        return midnight && (this == MONTH_1 ? utc.getDayOfMonth() == 1 : utc.getDayOfWeek() == DayOfWeek.MONDAY);
    }
}
