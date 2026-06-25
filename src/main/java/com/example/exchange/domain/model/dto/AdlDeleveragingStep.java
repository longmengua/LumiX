/*
 * 檔案用途：領域 DTO，承載 ADL forced deleveraging 的單一步驟。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * ADL deleveraging step。
 *
 * @param rank          ADL queue rank
 * @param uid           被強制減倉的使用者
 * @param symbol        normalized symbol code
 * @param reduceNotional 本步驟承擔的缺口名義金額
 * @param reduceQty     依 mark/notional 推導的減倉數量
 */
@Data
@Builder
@Jacksonized
public class AdlDeleveragingStep {

    private final int rank;

    private final long uid;

    private final String symbol;

    private final BigDecimal reduceNotional;

    private final BigDecimal reduceQty;
    public AdlDeleveragingStep(int rank, long uid, String symbol, BigDecimal reduceNotional, BigDecimal reduceQty) {
        this.rank = rank;
        this.uid = uid;
        this.symbol = symbol;
        this.reduceNotional = reduceNotional;
        this.reduceQty = reduceQty;
    }

    public int rank() {
        return rank;
    }

    public long uid() {
        return uid;
    }

    public String symbol() {
        return symbol;
    }

    public BigDecimal reduceNotional() {
        return reduceNotional;
    }

    public BigDecimal reduceQty() {
        return reduceQty;
    }
}