/*
 * 檔案用途：領域 DTO，承載 ADL forced deleveraging 的單一步驟。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

/**
 * ADL deleveraging step。
 *
 * @param rank          ADL queue rank
 * @param uid           被強制減倉的使用者
 * @param symbol        normalized symbol code
 * @param reduceNotional 本步驟承擔的缺口名義金額
 * @param reduceQty     依 mark/notional 推導的減倉數量
 */
public record AdlDeleveragingStep(
        int rank,
        long uid,
        String symbol,
        BigDecimal reduceNotional,
        BigDecimal reduceQty
) {
}
