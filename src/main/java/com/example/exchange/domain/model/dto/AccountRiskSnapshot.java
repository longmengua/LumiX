/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * 帳戶風險快照。
 *
 * <p>這個 DTO 是讀模型，不是帳務來源；用來讓 API / ops 快速觀察帳戶可用餘額、
 * frozen funds、維持保證金與風險率。持久化版本由 account risk snapshot store 保存。</p>
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
@Data
@Builder
@Jacksonized
public class AccountRiskSnapshot {

    private final long uid;

    private final BigDecimal crossBalance;

    private final BigDecimal availableBalance;

    private final BigDecimal orderHold;

    private final BigDecimal positionMargin;

    private final BigDecimal frozenFunds;

    private final BigDecimal unrealizedPnl;

    private final BigDecimal totalEquity;

    private final BigDecimal maintenanceMargin;

    private final BigDecimal riskRatio;

    private final int openPositionCount;

    private final Instant calculatedAt;
    public AccountRiskSnapshot(long uid, BigDecimal crossBalance, BigDecimal availableBalance, BigDecimal orderHold, BigDecimal positionMargin, BigDecimal frozenFunds, BigDecimal unrealizedPnl, BigDecimal totalEquity, BigDecimal maintenanceMargin, BigDecimal riskRatio, int openPositionCount, Instant calculatedAt) {
        this.uid = uid;
        this.crossBalance = crossBalance;
        this.availableBalance = availableBalance;
        this.orderHold = orderHold;
        this.positionMargin = positionMargin;
        this.frozenFunds = frozenFunds;
        this.unrealizedPnl = unrealizedPnl;
        this.totalEquity = totalEquity;
        this.maintenanceMargin = maintenanceMargin;
        this.riskRatio = riskRatio;
        this.openPositionCount = openPositionCount;
        this.calculatedAt = calculatedAt;
    }

    public long uid() {
        return uid;
    }

    public BigDecimal crossBalance() {
        return crossBalance;
    }

    public BigDecimal availableBalance() {
        return availableBalance;
    }

    public BigDecimal orderHold() {
        return orderHold;
    }

    public BigDecimal positionMargin() {
        return positionMargin;
    }

    public BigDecimal frozenFunds() {
        return frozenFunds;
    }

    public BigDecimal unrealizedPnl() {
        return unrealizedPnl;
    }

    public BigDecimal totalEquity() {
        return totalEquity;
    }

    public BigDecimal maintenanceMargin() {
        return maintenanceMargin;
    }

    public BigDecimal riskRatio() {
        return riskRatio;
    }

    public int openPositionCount() {
        return openPositionCount;
    }

    public Instant calculatedAt() {
        return calculatedAt;
    }
}