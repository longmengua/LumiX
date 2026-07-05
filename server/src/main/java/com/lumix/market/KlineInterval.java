package com.lumix.market;

import java.time.Duration;

/**
 * K 線週期。
 */
public enum KlineInterval {
    M1("1m", Duration.ofMinutes(1)),
    M5("5m", Duration.ofMinutes(5)),
    M15("15m", Duration.ofMinutes(15)),
    H1("1h", Duration.ofHours(1)),
    D1("1d", Duration.ofDays(1));

    private final String code;
    private final Duration duration;

    KlineInterval(String code, Duration duration) {
        this.code = code;
        this.duration = duration;
    }

    public String code() {
        return code;
    }

    public Duration duration() {
        return duration;
    }
}
