package com.lumix.trading.core.futures.margin;

import com.lumix.common.MoneyAmount;

import java.util.Objects;
import java.util.Optional;

/**
 * Isolated initial-margin sufficiency gate 的結果。
 *
 * 這個 result 只表達 gate decision 與 sandbox 計算結果。若 account / market / asset 一致性先失敗，
 * 會刻意不攜帶 notional 計算值，避免把未執行的計算偽裝成可信數字。
 */
public record IsolatedMarginCheckResult(
        FuturesMarginCheckStatus status,
        FuturesMarginCheckReason reason,
        Optional<MoneyAmount> requestedNotional,
        Optional<MoneyAmount> marginSupportedNotional
) {

    /**
     * 建立 approved result。
     *
     * 這裡只表示 sandbox capacity comparison 通過，不代表保證金已被凍結、order 已被接受或 position 已被建立。
     */
    public static IsolatedMarginCheckResult approved(MoneyAmount requestedNotional, MoneyAmount marginSupportedNotional) {
        return new IsolatedMarginCheckResult(
                FuturesMarginCheckStatus.APPROVED,
                FuturesMarginCheckReason.SUFFICIENT_MARGIN,
                Optional.of(requestedNotional),
                Optional.of(marginSupportedNotional)
        );
    }

    /**
     * 建立 insufficient-margin rejection。
     *
     * 這裡仍保留計算值，因為這種拒絕本身就是由容量比較導出的業務結果。
     */
    public static IsolatedMarginCheckResult insufficientMargin(MoneyAmount requestedNotional, MoneyAmount marginSupportedNotional) {
        return new IsolatedMarginCheckResult(
                FuturesMarginCheckStatus.REJECTED,
                FuturesMarginCheckReason.INSUFFICIENT_MARGIN,
                Optional.of(requestedNotional),
                Optional.of(marginSupportedNotional)
        );
    }

    /**
     * 建立一致性檢查失敗的 rejection。
     *
     * 這類結果故意不攜帶計算值，因為 gate 在 account / market / asset 邊界未通過時就應停止，
     * 避免用看似精確的 notional 掩蓋其實不可信的輸入組合。
     */
    public static IsolatedMarginCheckResult rejected(FuturesMarginCheckReason reason) {
        return new IsolatedMarginCheckResult(
                FuturesMarginCheckStatus.REJECTED,
                reason,
                Optional.empty(),
                Optional.empty()
        );
    }

    public IsolatedMarginCheckResult {
        // T04 不允許用 null 或半成品 result 表達「也許算過、也許沒算」，因此 constructor 直接鎖定 decision / reason / optional 邊界。
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(requestedNotional, "requestedNotional must not be null");
        Objects.requireNonNull(marginSupportedNotional, "marginSupportedNotional must not be null");

        boolean hasRequested = requestedNotional.isPresent();
        boolean hasSupported = marginSupportedNotional.isPresent();
        if (hasRequested != hasSupported) {
            throw new IllegalArgumentException("requestedNotional and marginSupportedNotional must both be present or both be empty");
        }
        requestedNotional.ifPresent(value -> {
            if (!value.isPositive()) {
                throw new IllegalArgumentException("requestedNotional must be greater than zero");
            }
        });
        marginSupportedNotional.ifPresent(value -> {
            if (value.isNegative()) {
                throw new IllegalArgumentException("marginSupportedNotional must not be negative");
            }
        });

        if (status == FuturesMarginCheckStatus.APPROVED && reason != FuturesMarginCheckReason.SUFFICIENT_MARGIN) {
            throw new IllegalArgumentException("APPROVED status must use SUFFICIENT_MARGIN reason");
        }
        if (reason == FuturesMarginCheckReason.SUFFICIENT_MARGIN && status != FuturesMarginCheckStatus.APPROVED) {
            throw new IllegalArgumentException("SUFFICIENT_MARGIN reason must use APPROVED status");
        }
        if (status == FuturesMarginCheckStatus.REJECTED && reason == FuturesMarginCheckReason.SUFFICIENT_MARGIN) {
            throw new IllegalArgumentException("REJECTED status must not use SUFFICIENT_MARGIN reason");
        }

        if (status == FuturesMarginCheckStatus.APPROVED) {
            if (!hasRequested) {
                throw new IllegalArgumentException("APPROVED result must include notional calculations");
            }
            if (marginSupportedNotional.get().compareTo(requestedNotional.get()) < 0) {
                throw new IllegalArgumentException("APPROVED result requires supported notional greater than or equal to requested notional");
            }
        } else if (reason == FuturesMarginCheckReason.INSUFFICIENT_MARGIN) {
            if (!hasRequested) {
                throw new IllegalArgumentException("INSUFFICIENT_MARGIN result must include notional calculations");
            }
            if (marginSupportedNotional.get().compareTo(requestedNotional.get()) >= 0) {
                throw new IllegalArgumentException("INSUFFICIENT_MARGIN result requires supported notional less than requested notional");
            }
        } else if (hasRequested) {
            throw new IllegalArgumentException("consistency rejections must not include notional calculations");
        }
    }
}
