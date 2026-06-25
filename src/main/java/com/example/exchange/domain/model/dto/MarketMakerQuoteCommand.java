/*
 * 檔案用途：做市商 quote command DTO，描述想要掛出的雙邊報價。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketMakerQuoteCommand {

    private final String marketMakerId;

    private final long uid;

    private final String symbol;

    private final BigDecimal bidPrice;

    private final BigDecimal bidQuantity;

    private final BigDecimal askPrice;

    private final BigDecimal askQuantity;

    private final String refId;
    public MarketMakerQuoteCommand(String marketMakerId, long uid, String symbol, BigDecimal bidPrice, BigDecimal bidQuantity, BigDecimal askPrice, BigDecimal askQuantity, String refId) {
        this.marketMakerId = marketMakerId;
        this.uid = uid;
        this.symbol = symbol;
        this.bidPrice = bidPrice;
        this.bidQuantity = bidQuantity;
        this.askPrice = askPrice;
        this.askQuantity = askQuantity;
        this.refId = refId;
    }

    public String marketMakerId() {
        return marketMakerId;
    }

    public long uid() {
        return uid;
    }

    public String symbol() {
        return symbol;
    }

    public BigDecimal bidPrice() {
        return bidPrice;
    }

    public BigDecimal bidQuantity() {
        return bidQuantity;
    }

    public BigDecimal askPrice() {
        return askPrice;
    }

    public BigDecimal askQuantity() {
        return askQuantity;
    }

    public String refId() {
        return refId;
    }
}