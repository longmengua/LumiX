/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class AdlQueueEntry {

    private final String liquidationId;

    private final long uid;

    private final String symbol;

    private final String liquidatedSide;

    private final BigDecimal amount;

    private final Instant ts;

    private final String status;

    private final String owner;

    private final Instant claimedAt;
    public AdlQueueEntry(String liquidationId, long uid, String symbol, String liquidatedSide, BigDecimal amount, Instant ts, String status, String owner, Instant claimedAt) {
        this.liquidationId = liquidationId;
        this.uid = uid;
        this.symbol = symbol;
        this.liquidatedSide = liquidatedSide;
        this.amount = amount;
        this.ts = ts;
        this.status = status;
        this.owner = owner;
        this.claimedAt = claimedAt;
    }

    public String liquidationId() {
        return liquidationId;
    }

    public long uid() {
        return uid;
    }

    public String symbol() {
        return symbol;
    }

    public String liquidatedSide() {
        return liquidatedSide;
    }

    public BigDecimal amount() {
        return amount;
    }

    public Instant ts() {
        return ts;
    }

    public String status() {
        return status;
    }

    public String owner() {
        return owner;
    }

    public Instant claimedAt() {
        return claimedAt;
    }
}