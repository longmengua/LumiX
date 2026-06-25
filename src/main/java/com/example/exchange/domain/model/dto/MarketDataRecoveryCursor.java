/*
 * 檔案用途：領域 DTO，描述 market-data client 斷線重連時可提交的恢復游標。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketDataRecoveryCursor {

    private final String symbol;

    private final long depthVersion;

    private final Instant tradeTs;

    private final String tradeMatchId;

    private final Instant generatedAt;
    public MarketDataRecoveryCursor(String symbol, long depthVersion, Instant tradeTs, String tradeMatchId, Instant generatedAt) {
        this.symbol = symbol;
        this.depthVersion = depthVersion;
        this.tradeTs = tradeTs;
        this.tradeMatchId = tradeMatchId;
        this.generatedAt = generatedAt;
    }

    public String symbol() {
        return symbol;
    }

    public long depthVersion() {
        return depthVersion;
    }

    public Instant tradeTs() {
        return tradeTs;
    }

    public String tradeMatchId() {
        return tradeMatchId;
    }

    public Instant generatedAt() {
        return generatedAt;
    }
}