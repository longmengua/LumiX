package com.lumix.withdrawal.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * 提款狀態唯一轉換門檻。
 *
 * <p>集中轉換可避免 service 任意 setStatus 使已廣播提款倒退回核准狀態；所有未知轉換皆拒絕。</p>
 */
public final class WithdrawalStateMachine {
    private static final Map<WithdrawalStatus, EnumSet<WithdrawalStatus>> NEXT = new EnumMap<>(WithdrawalStatus.class);
    static {
        allow(WithdrawalStatus.REQUESTED, WithdrawalStatus.FUNDS_HELD, WithdrawalStatus.FAILED, WithdrawalStatus.CANCELLED);
        allow(WithdrawalStatus.FUNDS_HELD, WithdrawalStatus.RISK_PENDING, WithdrawalStatus.FAILED, WithdrawalStatus.CANCELLED);
        allow(WithdrawalStatus.RISK_PENDING, WithdrawalStatus.APPROVED, WithdrawalStatus.MANUAL_REVIEW, WithdrawalStatus.RISK_HOLD, WithdrawalStatus.REJECTED, WithdrawalStatus.CANCELLED);
        allow(WithdrawalStatus.MANUAL_REVIEW, WithdrawalStatus.APPROVED, WithdrawalStatus.REJECTED, WithdrawalStatus.RISK_HOLD, WithdrawalStatus.CANCELLED);
        allow(WithdrawalStatus.APPROVED, WithdrawalStatus.FINAL_RISK_CHECK, WithdrawalStatus.CANCELLED);
        allow(WithdrawalStatus.FINAL_RISK_CHECK, WithdrawalStatus.SIGN_PENDING, WithdrawalStatus.RISK_HOLD, WithdrawalStatus.CANCELLED);
        allow(WithdrawalStatus.SIGN_PENDING, WithdrawalStatus.SIGNING, WithdrawalStatus.RISK_HOLD, WithdrawalStatus.CANCELLED);
        allow(WithdrawalStatus.SIGNING, WithdrawalStatus.SIGNED, WithdrawalStatus.RISK_HOLD, WithdrawalStatus.FAILED);
        allow(WithdrawalStatus.SIGNED, WithdrawalStatus.BROADCASTING, WithdrawalStatus.FAILED);
        allow(WithdrawalStatus.BROADCASTING, WithdrawalStatus.BROADCAST_UNKNOWN, WithdrawalStatus.BROADCASTED, WithdrawalStatus.FAILED);
        allow(WithdrawalStatus.BROADCAST_UNKNOWN, WithdrawalStatus.BROADCASTED, WithdrawalStatus.CONFIRMING, WithdrawalStatus.FAILED);
        allow(WithdrawalStatus.BROADCASTED, WithdrawalStatus.CONFIRMING, WithdrawalStatus.FAILED);
        allow(WithdrawalStatus.CONFIRMING, WithdrawalStatus.COMPLETED, WithdrawalStatus.FAILED);
        allow(WithdrawalStatus.RISK_HOLD, WithdrawalStatus.MANUAL_REVIEW, WithdrawalStatus.REJECTED, WithdrawalStatus.CANCELLED);
    }
    private static void allow(WithdrawalStatus from, WithdrawalStatus... to) { NEXT.put(from, EnumSet.of(to[0], to)); }
    /** 驗證狀態不能跳躍或倒退；終態沒有任何離開路徑。 */
    public void requireTransition(WithdrawalStatus from, WithdrawalStatus to) {
        if (from == null || to == null || !NEXT.getOrDefault(from, EnumSet.noneOf(WithdrawalStatus.class)).contains(to)) {
            throw new IllegalStateException("illegal withdrawal transition: " + from + " -> " + to);
        }
    }
}
