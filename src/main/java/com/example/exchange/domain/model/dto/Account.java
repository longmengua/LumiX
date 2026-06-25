/*
 * 檔案用途：領域模型或持久化實體，承載交易、帳戶、持倉與預測市場狀態。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    private static final String DEFAULT_CROSS_ASSET = "USDT";

    private long uid; // 使用者 ID

    // Legacy USDT cross fields remain as the contract margin view for existing recovery/risk code.
    private BigDecimal crossBalance = BigDecimal.ZERO;
    private BigDecimal crossAvailable = BigDecimal.ZERO;
    private BigDecimal crossOrderHold = BigDecimal.ZERO;
    private BigDecimal crossPositionMargin = BigDecimal.ZERO;

    // Spot requires per-asset balances; USDT mirrors the legacy cross fields for backward compatibility.
    private final Map<String, AssetBalance> assetBalances = new ConcurrentHashMap<>();

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
    public BigDecimal crossBalance() { syncLegacyFromDefaultAsset(); return crossBalance; }
    public BigDecimal crossAvailable() { syncLegacyFromDefaultAsset(); return crossAvailable; }
    public BigDecimal crossOrderHold() { syncLegacyFromDefaultAsset(); return crossOrderHold; }
    public BigDecimal crossPositionMargin() { syncLegacyFromDefaultAsset(); return crossPositionMargin; }
    public BigDecimal crossHold() { return crossOrderHold.add(crossPositionMargin); }
    public Instant updatedAt() { return updatedAt; }
    public BigDecimal balance(String asset) { return assetBalance(asset).balance; }
    public BigDecimal available(String asset) { return assetBalance(asset).available; }
    public BigDecimal orderHold(String asset) { return assetBalance(asset).orderHold; }
    public BigDecimal positionMargin(String asset) { return assetBalance(asset).positionMargin; }
    public Map<String, AssetBalance> assetBalances() {
        syncLegacyFromDefaultAsset();
        Map<String, AssetBalance> copy = new LinkedHashMap<>();
        assetBalances.keySet().stream().sorted().forEach(asset -> {
            AssetBalance balance = assetBalances.get(asset);
            copy.put(asset, new AssetBalance(
                    balance.balance,
                    balance.available,
                    balance.orderHold,
                    balance.positionMargin
            ));
        });
        return Collections.unmodifiableMap(copy);
    }
    public BigDecimal isolated(String sym) {
        return isolatedMargins.getOrDefault(sym, BigDecimal.ZERO);
    }

    /** 充值資金 → 加到 Cross Balance */
    public void deposit(BigDecimal amt) {
        deposit(DEFAULT_CROSS_ASSET, amt);
    }

    /** 充值指定資產；現貨與合約共用這個 per-asset hot state。 */
    public void deposit(String asset, BigDecimal amt) {
        requirePositive(amt, "deposit amount");
        AssetBalance balance = assetBalance(asset);
        balance.balance = balance.balance.add(amt);
        balance.available = balance.available.add(amt);
        syncLegacyIfDefault(asset, balance);
        updatedAt = Instant.now();
    }

    /** 提領資金 → 從 Cross Balance 扣除 */
    public void withdraw(BigDecimal amt) {
        withdraw(DEFAULT_CROSS_ASSET, amt);
    }

    /** 提領指定資產，必須只扣 available，不能動用已凍結資金。 */
    public void withdraw(String asset, BigDecimal amt) {
        requirePositive(amt, "withdraw amount");
        AssetBalance balance = assetBalance(asset);
        if (balance.available.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        balance.balance = balance.balance.subtract(amt);
        balance.available = balance.available.subtract(amt);
        syncLegacyIfDefault(asset, balance);
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
        reserveOrder(DEFAULT_CROSS_ASSET, amt);
    }

    public void reserveOrder(String asset, BigDecimal amt) {
        requirePositive(amt, "order reserve amount");
        AssetBalance balance = assetBalance(asset);
        if (balance.available.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient available balance for order reserve");
        }
        balance.available = balance.available.subtract(amt);
        balance.orderHold = balance.orderHold.add(amt);
        syncLegacyIfDefault(asset, balance);
        updatedAt = Instant.now();
    }

    public void releaseOrderReserve(BigDecimal amt) {
        releaseOrderReserve(DEFAULT_CROSS_ASSET, amt);
    }

    public void releaseOrderReserve(String asset, BigDecimal amt) {
        requirePositive(amt, "order release amount");
        AssetBalance balance = assetBalance(asset);
        if (balance.orderHold.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient order hold");
        }
        balance.orderHold = balance.orderHold.subtract(amt);
        balance.available = balance.available.add(amt);
        syncLegacyIfDefault(asset, balance);
        updatedAt = Instant.now();
    }

    public void convertOrderHoldToPositionMargin(BigDecimal amt) {
        convertOrderHoldToPositionMargin(DEFAULT_CROSS_ASSET, amt);
    }

    public void convertOrderHoldToPositionMargin(String asset, BigDecimal amt) {
        requirePositive(amt, "position margin amount");
        AssetBalance balance = assetBalance(asset);
        if (balance.orderHold.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient order hold for position margin");
        }
        balance.orderHold = balance.orderHold.subtract(amt);
        balance.positionMargin = balance.positionMargin.add(amt);
        syncLegacyIfDefault(asset, balance);
        updatedAt = Instant.now();
    }

    public void reservePositionMargin(BigDecimal amt) {
        reservePositionMargin(DEFAULT_CROSS_ASSET, amt);
    }

    public void reservePositionMargin(String asset, BigDecimal amt) {
        requirePositive(amt, "position margin amount");
        AssetBalance balance = assetBalance(asset);
        if (balance.available.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient available balance for position margin");
        }
        balance.available = balance.available.subtract(amt);
        balance.positionMargin = balance.positionMargin.add(amt);
        syncLegacyIfDefault(asset, balance);
        updatedAt = Instant.now();
    }

    public void releasePositionMargin(BigDecimal amt) {
        releasePositionMargin(DEFAULT_CROSS_ASSET, amt);
    }

    public void releasePositionMargin(String asset, BigDecimal amt) {
        requirePositive(amt, "position margin release amount");
        AssetBalance balance = assetBalance(asset);
        if (balance.positionMargin.compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient position margin");
        }
        balance.positionMargin = balance.positionMargin.subtract(amt);
        balance.available = balance.available.add(amt);
        syncLegacyIfDefault(asset, balance);
        updatedAt = Instant.now();
    }

    public void debit(BigDecimal amt) {
        debit(DEFAULT_CROSS_ASSET, amt);
    }

    public void debit(String asset, BigDecimal amt) {
        requirePositive(amt, "debit amount");
        AssetBalance balance = assetBalance(asset);
        BigDecimal fromAvailable = balance.available.min(amt);
        balance.available = balance.available.subtract(fromAvailable);

        BigDecimal remaining = amt.subtract(fromAvailable);
        if (remaining.signum() > 0) {
            BigDecimal fromOrderHold = balance.orderHold.min(remaining);
            balance.orderHold = balance.orderHold.subtract(fromOrderHold);
            remaining = remaining.subtract(fromOrderHold);
        }
        if (remaining.signum() > 0) {
            if (balance.positionMargin.compareTo(remaining) < 0) {
                throw new IllegalStateException("Insufficient cross balance");
            }
            balance.positionMargin = balance.positionMargin.subtract(remaining);
        }

        balance.balance = balance.balance.subtract(amt);
        syncLegacyIfDefault(asset, balance);
        updatedAt = Instant.now();
    }

    public void credit(BigDecimal amt) {
        credit(DEFAULT_CROSS_ASSET, amt);
    }

    public void credit(String asset, BigDecimal amt) {
        requirePositive(amt, "credit amount");
        AssetBalance balance = assetBalance(asset);
        balance.balance = balance.balance.add(amt);
        balance.available = balance.available.add(amt);
        syncLegacyIfDefault(asset, balance);
        updatedAt = Instant.now();
    }

    public void debitFromOrderHoldFirst(BigDecimal amt) {
        debitFromOrderHoldFirst(DEFAULT_CROSS_ASSET, amt);
    }

    public void debitFromOrderHoldFirst(String asset, BigDecimal amt) {
        requirePositive(amt, "debit amount");
        AssetBalance balance = assetBalance(asset);
        BigDecimal fromOrderHold = balance.orderHold.min(amt);
        balance.orderHold = balance.orderHold.subtract(fromOrderHold);

        BigDecimal remaining = amt.subtract(fromOrderHold);
        if (remaining.signum() > 0) {
            if (balance.available.compareTo(remaining) < 0) {
                throw new IllegalStateException("Insufficient balance for debit");
            }
            balance.available = balance.available.subtract(remaining);
        }

        balance.balance = balance.balance.subtract(amt);
        syncLegacyIfDefault(asset, balance);
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
        assetBalances.put(DEFAULT_CROSS_ASSET, new AssetBalance(
                crossBalance,
                crossAvailable,
                crossOrderHold,
                crossPositionMargin
        ));
        updatedAt = Instant.now();
    }

    private AssetBalance assetBalance(String asset) {
        String normalized = normalizeAsset(asset);
        return assetBalances.computeIfAbsent(normalized, ignored -> {
            if (DEFAULT_CROSS_ASSET.equals(normalized)) {
                return new AssetBalance(crossBalance, crossAvailable, crossOrderHold, crossPositionMargin);
            }
            return new AssetBalance(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        });
    }

    private void syncLegacyFromDefaultAsset() {
        AssetBalance balance = assetBalances.get(DEFAULT_CROSS_ASSET);
        if (balance == null) {
            return;
        }
        crossBalance = balance.balance;
        crossAvailable = balance.available;
        crossOrderHold = balance.orderHold;
        crossPositionMargin = balance.positionMargin;
    }

    private void syncLegacyIfDefault(String asset, AssetBalance balance) {
        if (!DEFAULT_CROSS_ASSET.equals(normalizeAsset(asset))) {
            return;
        }
        crossBalance = balance.balance;
        crossAvailable = balance.available;
        crossOrderHold = balance.orderHold;
        crossPositionMargin = balance.positionMargin;
    }

    private static String normalizeAsset(String asset) {
        if (asset == null || asset.isBlank()) {
            return DEFAULT_CROSS_ASSET;
        }
        return asset.trim().toUpperCase();
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

    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    @lombok.Data
    public static class AssetBalance {
        private BigDecimal balance = BigDecimal.ZERO;
        private BigDecimal available = BigDecimal.ZERO;
        private BigDecimal orderHold = BigDecimal.ZERO;
        private BigDecimal positionMargin = BigDecimal.ZERO;
    }
}
