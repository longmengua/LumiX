/*
 * 檔案用途：領域 DTO，承載 ADL queue ranking 的排序結果。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * ADL ranked position。
 *
 * @param rank              ADL queue rank，1 代表最優先被強制減倉
 * @param uid               使用者 ID
 * @param symbol            normalized symbol code
 * @param profitRate        未實現獲利率；越高越優先 ADL
 * @param effectiveLeverage 依 notional / margin 算出的有效槓桿；越高越優先 ADL
 * @param notional          倉位名義價值；排序 tie-breaker
 */
@Data
@Builder
@Jacksonized
public class AdlRankedPosition {

    private final int rank;

    private final long uid;

    private final String symbol;

    private final BigDecimal profitRate;

    private final BigDecimal effectiveLeverage;

    private final BigDecimal notional;
    public AdlRankedPosition(int rank, long uid, String symbol, BigDecimal profitRate, BigDecimal effectiveLeverage, BigDecimal notional) {
        this.rank = rank;
        this.uid = uid;
        this.symbol = symbol;
        this.profitRate = profitRate;
        this.effectiveLeverage = effectiveLeverage;
        this.notional = notional;
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

    public BigDecimal profitRate() {
        return profitRate;
    }

    public BigDecimal effectiveLeverage() {
        return effectiveLeverage;
    }

    public BigDecimal notional() {
        return notional;
    }
}