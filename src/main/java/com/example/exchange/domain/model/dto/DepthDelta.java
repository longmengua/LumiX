/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class DepthDelta {

    private final String symbol;

    private final long version;

    private final long checksum;

    private final List<PriceLevel> bids;

    private final List<PriceLevel> asks;

    private final Instant ts;
    public DepthDelta(String symbol, long version, long checksum, List<PriceLevel> bids, List<PriceLevel> asks, Instant ts) {
        this.symbol = symbol;
        this.version = version;
        this.checksum = checksum;
        this.bids = bids;
        this.asks = asks;
        this.ts = ts;
    }

    public String symbol() {
        return symbol;
    }

    public long version() {
        return version;
    }

    public long checksum() {
        return checksum;
    }

    public List<PriceLevel> bids() {
        return bids;
    }

    public List<PriceLevel> asks() {
        return asks;
    }

    public Instant ts() {
        return ts;
    }
}