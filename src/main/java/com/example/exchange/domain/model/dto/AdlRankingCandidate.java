/*
 * 檔案用途：領域 DTO，承載 ADL queue ranking 的候選倉位輸入。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * ADL ranking candidate。
 *
 * @param uid        使用者 ID
 * @param symbol     normalized symbol code
 * @param qty        倉位數量；正數 long、負數 short
 * @param entryPrice 倉位均價
 * @param markPrice  標記價格
 * @param margin     目前倉位保證金
 * @param leverage   倉位槓桿
 */
@Data
@Builder
@Jacksonized
public class AdlRankingCandidate {

    private final long uid;

    private final String symbol;

    private final BigDecimal qty;

    private final BigDecimal entryPrice;

    private final BigDecimal markPrice;

    private final BigDecimal margin;

    private final BigDecimal leverage;
    public AdlRankingCandidate(long uid, String symbol, BigDecimal qty, BigDecimal entryPrice, BigDecimal markPrice, BigDecimal margin, BigDecimal leverage) {
        this.uid = uid;
        this.symbol = symbol;
        this.qty = qty;
        this.entryPrice = entryPrice;
        this.markPrice = markPrice;
        this.margin = margin;
        this.leverage = leverage;
    }

    public long uid() {
        return uid;
    }

    public String symbol() {
        return symbol;
    }

    public BigDecimal qty() {
        return qty;
    }

    public BigDecimal entryPrice() {
        return entryPrice;
    }

    public BigDecimal markPrice() {
        return markPrice;
    }

    public BigDecimal margin() {
        return margin;
    }

    public BigDecimal leverage() {
        return leverage;
    }
}