/*
 * 檔案用途：Turnover 與 trade tape 對帳 issue DTO。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class TurnoverReconciliationIssue {

    private final String code;

    private final String message;

    private final long uid;

    private final String matchId;

    private final UUID orderId;

    private final String strategyId;

    private final String marketMakerId;

    private final boolean ledgerRefPresent;

    private final BigDecimal turnoverQuantity;

    private final BigDecimal tradeTapeQuantity;

    private final BigDecimal turnoverNotional;

    private final BigDecimal tradeTapeNotional;
    public TurnoverReconciliationIssue(String code, String message, long uid, String matchId, UUID orderId, String strategyId, String marketMakerId, boolean ledgerRefPresent, BigDecimal turnoverQuantity, BigDecimal tradeTapeQuantity, BigDecimal turnoverNotional, BigDecimal tradeTapeNotional) {
        this.code = code;
        this.message = message;
        this.uid = uid;
        this.matchId = matchId;
        this.orderId = orderId;
        this.strategyId = strategyId;
        this.marketMakerId = marketMakerId;
        this.ledgerRefPresent = ledgerRefPresent;
        this.turnoverQuantity = turnoverQuantity;
        this.tradeTapeQuantity = tradeTapeQuantity;
        this.turnoverNotional = turnoverNotional;
        this.tradeTapeNotional = tradeTapeNotional;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public long uid() {
        return uid;
    }

    public String matchId() {
        return matchId;
    }

    public UUID orderId() {
        return orderId;
    }

    public String strategyId() {
        return strategyId;
    }

    public String marketMakerId() {
        return marketMakerId;
    }

    public boolean ledgerRefPresent() {
        return ledgerRefPresent;
    }

    public BigDecimal turnoverQuantity() {
        return turnoverQuantity;
    }

    public BigDecimal tradeTapeQuantity() {
        return tradeTapeQuantity;
    }

    public BigDecimal turnoverNotional() {
        return turnoverNotional;
    }

    public BigDecimal tradeTapeNotional() {
        return tradeTapeNotional;
    }
}