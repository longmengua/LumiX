/*
 * 檔案用途：ADL queue 營運告警明細 DTO。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class AdlOperationalAlert {

    private final String alertType;

    private final String severity;

    private final String liquidationId;

    private final long uid;

    private final String symbol;

    private final BigDecimal amount;

    private final String status;

    private final String owner;

    private final long ageSeconds;

    private final String detail;
    public AdlOperationalAlert(String alertType, String severity, String liquidationId, long uid, String symbol, BigDecimal amount, String status, String owner, long ageSeconds, String detail) {
        this.alertType = alertType;
        this.severity = severity;
        this.liquidationId = liquidationId;
        this.uid = uid;
        this.symbol = symbol;
        this.amount = amount;
        this.status = status;
        this.owner = owner;
        this.ageSeconds = ageSeconds;
        this.detail = detail;
    }

    public String alertType() {
        return alertType;
    }

    public String severity() {
        return severity;
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

    public BigDecimal amount() {
        return amount;
    }

    public String status() {
        return status;
    }

    public String owner() {
        return owner;
    }

    public long ageSeconds() {
        return ageSeconds;
    }

    public String detail() {
        return detail;
    }
}