/*
 * 檔案用途：insurance fund capital movement read model。
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
public class InsuranceFundMovement {

    private final String movementId;

    private final String asset;

    private final String reason;

    private final String refId;

    private final BigDecimal amount;

    private final BigDecimal balanceAfter;

    private final Instant createdAt;
    public InsuranceFundMovement(String movementId, String asset, String reason, String refId, BigDecimal amount, BigDecimal balanceAfter, Instant createdAt) {
        this.movementId = movementId;
        this.asset = asset;
        this.reason = reason;
        this.refId = refId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    public String movementId() {
        return movementId;
    }

    public String asset() {
        return asset;
    }

    public String reason() {
        return reason;
    }

    public String refId() {
        return refId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public BigDecimal balanceAfter() {
        return balanceAfter;
    }

    public Instant createdAt() {
        return createdAt;
    }
}