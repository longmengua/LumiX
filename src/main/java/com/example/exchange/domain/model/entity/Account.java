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

    private final long uid; // 使用者 ID

    // Cross 資金池
    private BigDecimal crossBalance = BigDecimal.ZERO;
    private BigDecimal crossAvailable = BigDecimal.ZERO;

    // Isolated margin：以 symbol 為 key
    private final Map<String, BigDecimal> isolatedMargins = new ConcurrentHashMap<>();

    // 更新時間
    private Instant updatedAt = Instant.now();

    public Account(long uid) {
        this.uid = uid;
    }

    public long uid() { return uid; }
    public BigDecimal crossBalance() { return crossBalance; }
    public BigDecimal crossAvailable() { return crossAvailable; }
    public BigDecimal isolated(String sym) {
        return isolatedMargins.getOrDefault(sym, BigDecimal.ZERO);
    }

    /** 充值資金 → 加到 Cross Balance */
    public void deposit(BigDecimal amt) {
        crossBalance = crossBalance.add(amt);
        crossAvailable = crossAvailable.add(amt);
        updatedAt = Instant.now();
    }

    /** 提領資金 → 從 Cross Balance 扣除 */
    public void withdraw(BigDecimal amt) {
        if (crossAvailable.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        crossBalance = crossBalance.subtract(amt);
        crossAvailable = crossAvailable.subtract(amt);
        updatedAt = Instant.now();
    }

    /** 劃轉 → Cross → Isolated */
    public void moveToIsolated(String sym, BigDecimal amt) {
        if (crossAvailable.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient balance for isolated transfer");
        }
        crossAvailable = crossAvailable.subtract(amt);
        isolatedMargins.merge(sym, amt, BigDecimal::add);
        updatedAt = Instant.now();
    }

    /** 劃轉 → Isolated → Cross */
    public void moveFromIsolated(String sym, BigDecimal amt) {
        BigDecimal cur = isolatedMargins.getOrDefault(sym, BigDecimal.ZERO);
        if (cur.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient isolated margin");
        }
        isolatedMargins.put(sym, cur.subtract(amt));
        crossAvailable = crossAvailable.add(amt);
        updatedAt = Instant.now();
    }
}
