/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 帳戶風險快照。
 *
 * <p>這個 DTO 是讀模型，不是帳務來源；用來讓 API / ops 快速觀察帳戶可用餘額、
 * frozen funds、維持保證金與風險率。正式環境仍應把日終快照與 mark/index price
 * 來源獨立持久化。</p>
 *
 * @param uid 使用者 ID
 * @param crossBalance cross 總餘額
 * @param availableBalance 可用餘額
 * @param orderHold 委託預凍金額
 * @param positionMargin 持倉保證金
 * @param frozenFunds 委託預凍加持倉保證金
 * @param unrealizedPnl 未實現盈虧
 * @param totalEquity crossBalance + unrealizedPnl
 * @param maintenanceMargin 依目前 mark price 計算的維持保證金
 * @param riskRatio maintenanceMargin / totalEquity
 * @param openPositionCount 非零倉位數
 * @param calculatedAt 快照計算時間
 */
public record AccountRiskSnapshot(
        long uid,
        BigDecimal crossBalance,
        BigDecimal availableBalance,
        BigDecimal orderHold,
        BigDecimal positionMargin,
        BigDecimal frozenFunds,
        BigDecimal unrealizedPnl,
        BigDecimal totalEquity,
        BigDecimal maintenanceMargin,
        BigDecimal riskRatio,
        int openPositionCount,
        Instant calculatedAt
) {}
