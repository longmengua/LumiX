/*
 * 檔案用途：領域事件，描述交易、快照、資金費或強平等已發生的業務事實。
 */
package com.example.exchange.domain.event;

import com.example.exchange.domain.model.dto.Symbol;

import java.math.BigDecimal;
import java.time.Instant;

public record FundingSettled(
        long uid,
        Symbol symbol,
        BigDecimal markPrice,
        BigDecimal fundingRate,
        BigDecimal cashflow,
        String settlementId,
        Instant ts
) {}
