/*
 * 檔案用途：領域 DTO，承載 ADL queue ranking 的排序結果。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

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
public record AdlRankedPosition(
        int rank,
        long uid,
        String symbol,
        BigDecimal profitRate,
        BigDecimal effectiveLeverage,
        BigDecimal notional
) {
}
