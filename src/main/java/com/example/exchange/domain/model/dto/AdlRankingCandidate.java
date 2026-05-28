/*
 * 檔案用途：領域 DTO，承載 ADL queue ranking 的候選倉位輸入。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

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
public record AdlRankingCandidate(
        long uid,
        String symbol,
        BigDecimal qty,
        BigDecimal entryPrice,
        BigDecimal markPrice,
        BigDecimal margin,
        BigDecimal leverage
) {
}
