/*
 * 檔案用途：領域事件，記錄 liquidation / ADL 判斷資料以供 audit。
 */
package com.example.exchange.domain.event;

import com.example.exchange.domain.model.entity.Symbol;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Liquidation decision audit event。
 *
 * @param uid               使用者 ID
 * @param symbol            交易對
 * @param liquidationId     本次判斷 ID
 * @param liquidated        是否觸發 liquidation
 * @param reason            判斷原因
 * @param markPrice         標記價格
 * @param maintenanceMargin 維持保證金需求
 * @param equity            帳戶權益
 * @param insuranceCovered  保險基金覆蓋金額
 * @param adlCovered        ADL / 社會化缺口金額
 * @param decidedAt         判斷時間
 */
public record LiquidationDecisionRecorded(
        long uid,
        Symbol symbol,
        String liquidationId,
        boolean liquidated,
        String reason,
        BigDecimal markPrice,
        BigDecimal maintenanceMargin,
        BigDecimal equity,
        BigDecimal insuranceCovered,
        BigDecimal adlCovered,
        Instant decidedAt
) {
}
