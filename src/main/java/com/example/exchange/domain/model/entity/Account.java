package com.example.exchange.domain.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Account (帳戶聚合根)
 *
 * - 每個使用者(uid)一個 Account
 * - Cross 模式：共用一個資金池 crossBalance
 * - Isolated 模式：每個 symbol 獨立 margin
 *
 * 提供基本操作：
 *   - deposit / withdraw
 *   - moveToIsolated / moveFromIsolated
 */
public class Account {

    private long uid; // 使用者 ID

    // Cross 資金池
    private BigDecimal crossBalance = BigDecimal.ZERO;
    private BigDecimal crossAvailable = BigDecimal.ZERO;
    private BigDecimal crossOrderHold = BigDecimal.ZERO;
    private BigDecimal crossPositionMargin = BigDecimal.ZERO;

    // Isolated margin：以 symbol 為 key
    private final Map<String, BigDecimal> isolatedMargins = new ConcurrentHashMap<>();

    // 更新時間
    private Instant updatedAt = Instant.now();

    public Account() {
    }

    public Account(long uid) {
        this.uid = uid;
    }

    public long uid() { return uid; }
    public BigDecimal crossBalance() { return crossBalance; }
    public BigDecimal crossAvailable() { return crossAvailable; }
    public BigDecimal crossOrderHold() { return crossOrderHold; }
    public BigDecimal crossPositionMargin() { return crossPositionMargin; }
    public BigDecimal crossHold() { return crossOrderHold.add(crossPositionMargin); }
    public Instant updatedAt() { return updatedAt; }
    public BigDecimal isolated(String sym) {
        return isolatedMargins.getOrDefault(sym, BigDecimal.ZERO);
    }

    /** 充值資金 → 加到 Cross Balance */
    public void deposit(BigDecimal amt) {
        requirePositive(amt, "deposit amount");
        crossBalance = crossBalance.add(amt);
        crossAvailable = crossAvailable.add(amt);
        updatedAt = Instant.now();
    }

    /** 提領資金 → 從 Cross Balance 扣除 */
    public void withdraw(BigDecimal amt) {
        requirePositive(amt, "withdraw amount");
        if (crossAvailable.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        crossBalance = crossBalance.subtract(amt);
        crossAvailable = crossAvailable.subtract(amt);
        updatedAt = Instant.now();
    }

    /** 劃轉 → Cross → Isolated */
    public void moveToIsolated(String sym, BigDecimal amt) {
        requirePositive(amt, "isolated transfer amount");
        if (crossAvailable.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient balance for isolated transfer");
        }
        crossAvailable = crossAvailable.subtract(amt);
        isolatedMargins.merge(sym, amt, BigDecimal::add);
        updatedAt = Instant.now();
    }

    /** 劃轉 → Isolated → Cross */
    public void moveFromIsolated(String sym, BigDecimal amt) {
        requirePositive(amt, "isolated transfer amount");
        BigDecimal cur = isolatedMargins.getOrDefault(sym, BigDecimal.ZERO);
        if (cur.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient isolated margin");
        }
        isolatedMargins.put(sym, cur.subtract(amt));
        crossAvailable = crossAvailable.add(amt);
        updatedAt = Instant.now();
    }

    public void reserveOrder(BigDecimal amt) {
        requirePositive(amt, "order reserve amount");
        if (crossAvailable.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient available balance for order reserve");
        }
        crossAvailable = crossAvailable.subtract(amt);
        crossOrderHold = crossOrderHold.add(amt);
        updatedAt = Instant.now();
    }

    public void releaseOrderReserve(BigDecimal amt) {
        requirePositive(amt, "order release amount");
        if (crossOrderHold.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient order hold");
        }
        crossOrderHold = crossOrderHold.subtract(amt);
        crossAvailable = crossAvailable.add(amt);
        updatedAt = Instant.now();
    }

    public void convertOrderHoldToPositionMargin(BigDecimal amt) {
        requirePositive(amt, "position margin amount");
        if (crossOrderHold.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient order hold for position margin");
        }
        crossOrderHold = crossOrderHold.subtract(amt);
        crossPositionMargin = crossPositionMargin.add(amt);
        updatedAt = Instant.now();
    }

    public void reservePositionMargin(BigDecimal amt) {
        requirePositive(amt, "position margin amount");
        if (crossAvailable.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient available balance for position margin");
        }
        crossAvailable = crossAvailable.subtract(amt);
        crossPositionMargin = crossPositionMargin.add(amt);
        updatedAt = Instant.now();
    }

    public void releasePositionMargin(BigDecimal amt) {
        requirePositive(amt, "position margin release amount");
        if (crossPositionMargin.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient position margin");
        }
        crossPositionMargin = crossPositionMargin.subtract(amt);
        crossAvailable = crossAvailable.add(amt);
        updatedAt = Instant.now();
    }

    public void debit(BigDecimal amt) {
        requirePositive(amt, "debit amount");
        BigDecimal fromAvailable = crossAvailable.min(amt);
        crossAvailable = crossAvailable.subtract(fromAvailable);

        BigDecimal remaining = amt.subtract(fromAvailable);
        if (remaining.signum() > 0) {
            BigDecimal fromOrderHold = crossOrderHold.min(remaining);
            crossOrderHold = crossOrderHold.subtract(fromOrderHold);
            remaining = remaining.subtract(fromOrderHold);
        }
        if (remaining.signum() > 0) {
            if (crossPositionMargin.compareTo(remaining) < 0) {
                throw new IllegalStateException("Insufficient cross balance");
            }
            crossPositionMargin = crossPositionMargin.subtract(remaining);
        }

        crossBalance = crossBalance.subtract(amt);
        updatedAt = Instant.now();
    }

    public void credit(BigDecimal amt) {
        requirePositive(amt, "credit amount");
        crossBalance = crossBalance.add(amt);
        crossAvailable = crossAvailable.add(amt);
        updatedAt = Instant.now();
    }

    public void debitFromOrderHoldFirst(BigDecimal amt) {
        requirePositive(amt, "debit amount");
        BigDecimal fromOrderHold = crossOrderHold.min(amt);
        crossOrderHold = crossOrderHold.subtract(fromOrderHold);

        BigDecimal remaining = amt.subtract(fromOrderHold);
        if (remaining.signum() > 0) {
            if (crossAvailable.compareTo(remaining) < 0) {
                throw new IllegalStateException("Insufficient balance for debit");
            }
            crossAvailable = crossAvailable.subtract(remaining);
        }

        crossBalance = crossBalance.subtract(amt);
        updatedAt = Instant.now();
    }

    public void restoreCross(
            BigDecimal balance,
            BigDecimal available,
            BigDecimal orderHold,
            BigDecimal positionMargin
    ) {
        crossBalance = nonNegative(balance, "cross balance");
        crossAvailable = nonNegative(available, "cross available");
        crossOrderHold = nonNegative(orderHold, "cross order hold");
        crossPositionMargin = nonNegative(positionMargin, "cross position margin");
        updatedAt = Instant.now();
    }

    private static void requirePositive(BigDecimal amt, String label) {
        if (amt == null || amt.signum() <= 0) {
            throw new IllegalArgumentException(label + " must be positive");
        }
    }

    private static BigDecimal nonNegative(BigDecimal amt, String label) {
        if (amt == null) return BigDecimal.ZERO;
        if (amt.signum() < 0) {
            throw new IllegalArgumentException(label + " must be non-negative");
        }
        return amt;
    }
}
